package kr.inuappcenterportal.inuportal.domain.semester.converter;

import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class SemesterTermConverter implements Converter<String, SemesterTerm> {

    @Override
    public SemesterTerm convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return SemesterTerm.from(source);
    }
}
