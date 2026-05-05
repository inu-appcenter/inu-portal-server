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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;
    private final ChatRedisService chatRedisService;
    private final ObjectMapper objectMapper;

    @Value("${jwtSecret}")
    private String salt;

    public String getSenderHash(Long memberId) {
        return DigestUtils.md5DigestAsHex((memberId + salt).getBytes());
    }

    @Transactional
    public ChatRoomResponseDto createChatRoom(ChatRoomCreateRequestDto requestDto, Long memberId) {
        Member creator = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        ChatRoom chatRoom = ChatRoom.builder()
                .title(requestDto.getTitle())
                .maxCapacity(requestDto.getMaxCapacity())
                .isAnonymous(requestDto.getIsAnonymous())
                .build();
        chatRoomRepository.save(chatRoom);

        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .member(creator)
                .build();
        chatRoomMemberRepository.save(chatRoomMember);

        chatRedisService.addUserToRoom(chatRoom.getId(), memberId);

        return ChatRoomResponseDto.of(chatRoom, 1, getSenderHash(memberId));
    }

    @Transactional
    public ChatRoomResponseDto joinChatRoom(Long roomId, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        Long currentParticipants = chatRedisService.getRoomUserCount(roomId);

        if (chatRoomMemberRepository.existsByChatRoomAndMember(chatRoom, member)) {
            return ChatRoomResponseDto.of(chatRoom, currentParticipants.intValue(), getSenderHash(memberId));
        }

        if (currentParticipants >= chatRoom.getMaxCapacity()) {
            throw new MyException(MyErrorCode.CHATROOM_FULL);
        }

        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .member(member)
                .build();
        chatRoomMemberRepository.save(chatRoomMember);

        chatRedisService.addUserToRoom(roomId, memberId);

        return ChatRoomResponseDto.of(chatRoom, currentParticipants.intValue() + 1, getSenderHash(memberId));
    }

    @Transactional(readOnly = true)
    public ChatRoomResponseDto getChatRoomMessages(Long roomId, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        if (!chatRoomMemberRepository.existsByChatRoomAndMember(chatRoom, member)) {
            throw new MyException(MyErrorCode.NOT_CHATROOM_MEMBER);
        }

        List<ChatMessageResponseDto> messages = new ArrayList<>();
        List<String> cachedMessagesJson = chatRedisService.getRecentMessages(roomId);

        for (String messageJson : cachedMessagesJson) {
            try {
                messages.add(objectMapper.readValue(messageJson, ChatMessageResponseDto.class));
            } catch (JsonProcessingException e) {
                log.error("Error deserializing cached message: {}", messageJson, e);
            }
        }

        messages.sort(Comparator.comparing(ChatMessageResponseDto::getCreateDate));

        if (messages.isEmpty()) {
            List<ChatMessage> dbMessages = chatMessageRepository.findTop50ByChatRoomOrderByCreateDateDesc(chatRoom);
            messages.addAll(dbMessages.stream()
                    .map(ChatMessageResponseDto::of)
                    .collect(Collectors.toList()));
            messages.sort(Comparator.comparing(ChatMessageResponseDto::getCreateDate)); // DB 메시지도 정렬
        }

        Long currentParticipants = chatRedisService.getRoomUserCount(roomId);
        String myHash = getSenderHash(memberId);

        return ChatRoomResponseDto.of(chatRoom, currentParticipants.intValue(), myHash, messages);
    }
}
