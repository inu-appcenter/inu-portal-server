package kr.inuappcenterportal.inuportal.global.exception.ex;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MyErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND,"존재하지 않는 유저입니다."),
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND,"존재하지 않는 이미지 번호입니다."),
    POST_SCRAP_LIST_NOT_FOUND(HttpStatus.NOT_FOUND,"게시글 리스트가 비어있습니다."),
    SCRAP_NOT_FOUND(HttpStatus.NOT_FOUND,"스크랩하지 않은 게시물입니다."),
    FOLDER_NOT_FOUND(HttpStatus.NOT_FOUND,"존재하지 않는 스크랩폴더입니다."),
    FOLDER_OR_POST_NOT_FOUND(HttpStatus.NOT_FOUND,"스크랩폴더나 게시글이 존재하지 않습니다."),
    USER_OR_POST_NOT_FOUND(HttpStatus.NOT_FOUND,"유저나 게시글이 존재하지 않습니다."),
    USER_OR_REPLY_NOT_FOUND(HttpStatus.NOT_FOUND,"유저나 댓글이 존재하지 않습니다."),
    NOT_REPLY_ON_REREPLY(HttpStatus.BAD_REQUEST,"대댓글에 댓글을 작성할 수 없습니다."),
    REPLY_NOT_FOUND(HttpStatus.NOT_FOUND,"존재하지 않는 댓글입니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND,"존재하지 않는 카테고리입니다."),
    USER_DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST,"동일한 이메일이 존재합니다."),
    ONLY_SCHOOL_EMAIL(HttpStatus.BAD_REQUEST,"학교 이메일만 가입 가능합니다."),
    POST_DUPLICATE_FOLDER(HttpStatus.BAD_REQUEST,"스크랩폴더에 존재하는 게시글입니다."),
    USER_DUPLICATE_NICKNAME(HttpStatus.BAD_REQUEST,"동일한 닉네임이 존재합니다."),
    SAME_NICKNAME_UPDATE(HttpStatus.BAD_REQUEST,"입력한 닉네임과 현재 닉네임이 동일합니다."),
    USER_DUPLICATE_CATEGORY(HttpStatus.BAD_REQUEST,"동일한 카테고리가 존재합니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND,"존재하지 않는 게시글입니다."),
    ID_NOT_FOUND(HttpStatus.UNAUTHORIZED,"아이디가 존재하지 않습니다."),
    PASSWORD_NOT_MATCHED(HttpStatus.UNAUTHORIZED,"비밀번호가 틀립니다."),
    HAS_NOT_POST_AUTHORIZATION(HttpStatus.FORBIDDEN,"이 게시글의 수정/삭제에 대한 권한이 없습니다."),
    HAS_NOT_REPLY_AUTHORIZATION(HttpStatus.FORBIDDEN,"이 댓글의 수정/삭제에 대한 권한이 없습니다."),
    NOT_MULTIPLE_LIKE(HttpStatus.CONFLICT,"이미 좋아요를 눌렀습니다."),
    NOT_MULTIPLE_DISLIKE(HttpStatus.CONFLICT,"이미 싫어요를 눌렀습니다."),
    WRONG_TYPE_TOKEN(HttpStatus.UNAUTHORIZED,"토큰의 서명이 유효하지 않습니다."),
    UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED,"잘못된 형식의 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED,"만료된 토큰입니다."),
    UNKNOWN_TOKEN_ERROR(HttpStatus.BAD_REQUEST,"토큰의 값이 존재하지 않습니다."),
    WRONG_SORT_TYPE(HttpStatus.BAD_REQUEST,"정렬의 기준값이 올바르지 않습니다."),
    WRONG_SEARCH_TYPE(HttpStatus.BAD_REQUEST,"검색옵션이 올바르지 않습니다."),
    EMPTY_REQUEST(HttpStatus.BAD_REQUEST,"닉네임, 횃불이 아이디 모두 공백입니다."),
    NOT_BLANK_NICKNAME(HttpStatus.BAD_REQUEST,"닉네임이 빈칸 혹은 공백입니다."),
    EMAIL_NOT_AUTHORIZATION(HttpStatus.FORBIDDEN,"인증되지 않은 이메일입니다."),
    NOT_LIKE_MY_POST(HttpStatus.BAD_REQUEST,"자신의 게시글에는 추천을 할 수 없습니다."),
    NOT_LIKE_MY_REPLY(HttpStatus.BAD_REQUEST,"자신의 댓글에는 추천을 할 수 없습니다."),
    BAD_REQUEST_FIRE_AI(HttpStatus.BAD_REQUEST,"횃불이 이미지 관련 요청에 문제가 있습니다."),
    AI_IMAGE_GENERATING(HttpStatus.BAD_REQUEST,"횃불이 이미지가 생성 중 입니다."),
    STUDENT_LOGIN_ERROR(HttpStatus.UNAUTHORIZED,"학번 또는 비밀번호가 틀립니다."),
    RATED_IMAGE(HttpStatus.BAD_REQUEST,"이미 평가된 이미지입니다."),
    WEATHER_REQUEST_ERROR(HttpStatus.BAD_REQUEST,"날씨 요청에 문제가 있습니다."),
    BLOCK_MANY_SAME_POST_REPLY(HttpStatus.BAD_REQUEST,"일정 시간 동안 같은 게시글이나 댓글을 작성할 수 없습니다."),
    NOT_GENERATE_FIRE_AI_IMAGE(HttpStatus.BAD_REQUEST,"횃불이 이미지가 생성 중 입니다.");


    private final HttpStatus status;
    private final String message;
}
