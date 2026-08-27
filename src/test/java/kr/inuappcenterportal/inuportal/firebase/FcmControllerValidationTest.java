package kr.inuappcenterportal.inuportal.firebase;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.executable.ExecutableValidator;
import kr.inuappcenterportal.inuportal.domain.firebase.contorller.FcmController;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 컨트롤러가 {@code FcmApiSpecification}을 구현하므로, 파라미터 제약은 인터페이스에만 선언해야 한다.
 * 구현 메서드가 제약을 다시 선언하면 Bean Validation이 메타데이터를 만들 때
 * {@code ConstraintDeclarationException: HV000151}로 거부해 요청이 500으로 떨어진다.
 */
class FcmControllerValidationTest {

    private final ExecutableValidator executableValidator = validator();

    @Test
    @DisplayName("페이지 파라미터를 받는 메서드의 검증 메타데이터가 예외 없이 만들어진다")
    void buildsValidationMetadataWithoutRedeclaringConstraints() {
        assertThatCode(() -> {
            validateParameters("checkNotification", new Class<?>[]{Member.class, int.class}, null, 1);
            validateParameters("readPageNotification", new Class<?>[]{Member.class, int.class}, null, 1);
            validateParameters("countAdminFcmMessagesSuccess", new Class<?>[]{int.class}, 1);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("제약을 인터페이스로 옮긴 뒤에도 page 최솟값 검증은 그대로 동작한다")
    void stillRejectsPageBelowOne() {
        assertThat(validateParameters("checkNotification", new Class<?>[]{Member.class, int.class}, null, 0))
                .hasSize(1);
        assertThat(validateParameters("readPageNotification", new Class<?>[]{Member.class, int.class}, null, 0))
                .hasSize(1);
        assertThat(validateParameters("countAdminFcmMessagesSuccess", new Class<?>[]{int.class}, 0))
                .hasSize(1);
    }

    @Test
    @DisplayName("유효한 페이지 값은 통과한다")
    void acceptsValidPage() {
        assertThat(validateParameters("checkNotification", new Class<?>[]{Member.class, int.class}, null, 1))
                .isEmpty();
    }

    private java.util.Set<? extends jakarta.validation.ConstraintViolation<FcmController>> validateParameters(
            String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            FcmController controller = new FcmController(null, null);
            Method method = FcmController.class.getMethod(methodName, parameterTypes);
            return executableValidator.validateParameters(controller, method, args);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ExecutableValidator validator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            return factory.getValidator().forExecutables();
        }
    }
}
