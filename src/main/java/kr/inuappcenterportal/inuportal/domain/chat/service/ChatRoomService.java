package kr.inuappcenterportal.inuportal.domain.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatMessage;
import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatRoom;
import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatRoomMember;
import kr.inuappcenterportal.inuportal.domain.chat.dto.ChatMessageResponseDto;
import kr.inuappcenterportal.inuportal.domain.chat.dto.ChatRoomCreateRequestDto;
import kr.inuappcenterportal.inuportal.domain.chat.dto.ChatRoomResponseDto;
import kr.inuappcenterportal.inuportal.domain.chat.repository.ChatMessageRepository;
import kr.inuappcenterportal.inuportal.domain.chat.repository.ChatRoomMemberRepository;
import kr.inuappcenterportal.inuportal.domain.chat.repository.ChatRoomRepository;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository; // Member 엔티티 조회를 위해 필요
    private final ChatRedisService chatRedisService;
    private final ObjectMapper objectMapper; // JSON 직렬화를 위해 필요

    @Transactional
    public ChatRoomResponseDto createChatRoom(ChatRoomCreateRequestDto requestDto, Long memberId) {
        Member creator = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND)); // NOT_FOUND_MEMBER -> USER_NOT_FOUND

        ChatRoom chatRoom = ChatRoom.builder()
                .title(requestDto.getTitle())
                .maxCapacity(requestDto.getMaxCapacity())
                .isAnonymous(requestDto.getIsAnonymous())
                .build();
        chatRoomRepository.save(chatRoom);

        // 방 생성자는 자동으로 참여자로 등록
        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .member(creator)
                .build();
        chatRoomMemberRepository.save(chatRoomMember);

        // Redis에 참여자 추가
        chatRedisService.addUserToRoom(chatRoom.getId(), memberId);

        return ChatRoomResponseDto.of(chatRoom, 1); // 생성 시 참여자 수는 1명
    }

    @Transactional
    public ChatRoomResponseDto joinChatRoom(Long roomId, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND)); // NOT_FOUND_MEMBER -> USER_NOT_FOUND

        // 이미 참여 중인지 확인
        if (chatRoomMemberRepository.existsByChatRoomAndMember(chatRoom, member)) {
            throw new MyException(MyErrorCode.ALREADY_JOINED_CHATROOM);
        }

        // 최대 인원 초과 여부 확인 (Redis에서 실시간 인원 조회)
        Long currentParticipants = chatRedisService.getRoomUserCount(roomId);
        if (currentParticipants >= chatRoom.getMaxCapacity()) {
            throw new MyException(MyErrorCode.CHATROOM_FULL);
        }

        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .member(member)
                .build();
        chatRoomMemberRepository.save(chatRoomMember);

        // Redis에 참여자 추가
        chatRedisService.addUserToRoom(roomId, memberId);

        return ChatRoomResponseDto.of(chatRoom, currentParticipants.intValue() + 1);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponseDto> getChatRoomMessages(Long roomId, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND)); // NOT_FOUND_MEMBER -> USER_NOT_FOUND

        // 해당 유저가 채팅방에 참여 중인지 확인
        if (!chatRoomMemberRepository.existsByChatRoomAndMember(chatRoom, member)) {
            throw new MyException(MyErrorCode.NOT_CHATROOM_MEMBER);
        }

        List<ChatMessageResponseDto> messages = new ArrayList<>();

        // 1. Redis에서 최근 50개 메시지 로드
        List<String> cachedMessagesJson = chatRedisService.getRecentMessages(roomId);
        for (String messageJson : cachedMessagesJson) {
            try {
                messages.add(objectMapper.readValue(messageJson, ChatMessageResponseDto.class));
            } catch (JsonProcessingException e) {
                log.error("Error deserializing cached message: {}", messageJson, e);
            }
        }
        
        // 메시지 순서를 오래된 것부터 최신 순으로 정렬 (Redis는 역순으로 저장될 수 있음)
        messages.sort((m1, m2) -> m1.getCreateDate().compareTo(m2.getCreateDate())); // getCreatedAt() -> getCreateDate()

        // Redis 메시지가 비어있거나 부족할 경우 DB에서 추가 로드 (선택적)
        // TODO: Redis 캐시와 DB 메시지 목록을 병합하는 로직은 추후 필요에 따라 고도화 (예: Redis 메시지 ID를 기준으로 DB에서 이전 메시지 조회)
        if (messages.isEmpty()) { // Redis에 메시지가 없다면 DB에서 최신 50개 가져오기
            List<ChatMessage> dbMessages = chatMessageRepository.findTop50ByChatRoomOrderByCreateDateDesc(chatRoom); // findTop50ByChatRoomOrderByCreatedAtDesc -> findTop50ByChatRoomOrderByCreateDateDesc
            messages.addAll(dbMessages.stream()
                    .map(ChatMessageResponseDto::of)
                    .collect(Collectors.toList()));
        }
        
        return messages;
    }
}
