package kr.inuappcenterportal.inuportal.domain.chat.service;

import kr.inuappcenterportal.inuportal.domain.chat.model.ChatRoom;
import kr.inuappcenterportal.inuportal.domain.chat.model.ChatRoomMember;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatMessage;
import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatRoom;
import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatRoomMember;
import kr.inuappcenterportal.inuportal.domain.chat.dto.ChatMessageRequestDto;
import kr.inuappcenterportal.inuportal.domain.chat.dto.ChatMessageResponseDto;
import kr.inuappcenterportal.inuportal.domain.chat.dto.ChatRoomCreateRequestDto;
import kr.inuappcenterportal.inuportal.domain.chat.dto.ChatRoomResponseDto;
import kr.inuappcenterportal.inuportal.domain.chat.dto.PublicChatMessageResponseDto;
import kr.inuappcenterportal.inuportal.domain.chat.repository.ChatMessageRepository;
import kr.inuappcenterportal.inuportal.domain.chat.repository.ChatRoomMemberRepository;
import kr.inuappcenterportal.inuportal.domain.chat.repository.ChatRoomRepository;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.domain.image.service.ImageService;
import io.hypersistence.tsid.TSID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MemberRepository memberRepository;

    /**
     * 특정 채팅방에 속한 모든 멤버를 조회합니다.
     * @param roomId 채팅방 이름 (String)
     * @return 해당 채팅방의 멤버 리스트
     */
    public List<Member> getRoomMembers(String roomId) {
        return chatRoomRepository.findByName(roomId)
                .map(chatRoom -> chatRoomMemberRepository.findAllMembersByChatRoomId(chatRoom.getId()))
                .orElse(Collections.emptyList());
    }

    /**
     * 테스트용 채팅방 및 멤버 생성
     * @param roomName 생성할 채팅방 이름
     * @param memberIds 채팅방에 추가할 멤버 ID 목록
     * @return 생성된 ChatRoom
     */
    @Transactional
    public ChatRoom createTestChatRoom(String roomName, List<Long> memberIds) {
        ChatRoom chatRoom = chatRoomRepository.findByName(roomName)
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.builder().name(roomName).build()));

        for (Long memberId : memberIds) {
            memberRepository.findById(memberId).ifPresent(member -> {
                // 이미 해당 채팅방에 멤버가 있는지 확인 (중복 방지)
                boolean alreadyMember = chatRoomMemberRepository.findAllMembersByChatRoomId(chatRoom.getId())
                        .stream()
                        .anyMatch(m -> m.getId().equals(memberId));
                
                if (!alreadyMember) {
                    chatRoomMemberRepository.save(ChatRoomMember.builder()
                            .chatRoom(chatRoom)
                            .member(member)
                            .build());
                }
            });
        }
        return chatRoom;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;
    private final ChatRedisService chatRedisService;
    private final ChatBatchService chatBatchService;
    private final ImageService imageService;
    private final SimpMessageSendingOperations messagingTemplate;
    private final ObjectMapper objectMapper;

    @Value("${jwtSecret}")
    private String salt;

    @Value("${chatImagePath}")
    private String chatImagePath;

    public String getSenderHash(Long memberId) {
        return DigestUtils.md5DigestAsHex((memberId + salt).getBytes());
    }

    @Transactional
    public void sendMessage(ChatMessageRequestDto messageDto, Long memberId) {
        Member sender = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        String nickname;
        if (messageDto.getIsAnonymous()) {
            nickname = chatRedisService.getOrAssignAnonymousNickname(messageDto.getRoomId(), memberId);
        } else {
            nickname = sender.getNickname();
        }

        LocalDateTime now = LocalDateTime.now();
        String senderHash = getSenderHash(memberId);
        Long messageId = TSID.fast().toLong();

        ChatMessageResponseDto responseDto = ChatMessageResponseDto.builder()
                .messageId(messageId)
                .roomId(messageDto.getRoomId())
                .senderNickname(nickname)
                .senderHash(senderHash)
                .content(messageDto.getContent())
                .imageCount(messageDto.getImageCount())
                .createDate(now)
                .build();

        // 브로드캐스팅 및 캐싱 로직은 별도 메서드로 분리하면 좋을 듯
        broadcastAndCache(messageDto.getRoomId(), responseDto);

        ChatRoom chatRoom = chatRoomRepository.findById(messageDto.getRoomId())
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));

        ChatMessage chatMessage = ChatMessage.builder()
                .id(messageId)
                .chatRoom(chatRoom)
                .sender(sender)
                .content(messageDto.getContent())
                .senderNickname(nickname)
                .imageCount(messageDto.getImageCount())
                .createDate(now)
                .modifiedDate(now)
                .build();

        chatBatchService.addMessageToQueue(chatMessage);
    }

    @Transactional
    public ChatMessageResponseDto sendMessageWithImages(ChatMessageRequestDto messageDto, List<MultipartFile> images, Long memberId) throws IOException {
        Member sender = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        ChatRoom chatRoom = chatRoomRepository.findById(messageDto.getRoomId())
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));

        String nickname = messageDto.getIsAnonymous()
                ? chatRedisService.getOrAssignAnonymousNickname(messageDto.getRoomId(), memberId)
                : sender.getNickname();

        Long messageId = TSID.fast().toLong();
        LocalDateTime now = LocalDateTime.now();

        ChatMessage chatMessage = ChatMessage.builder()
                .id(messageId)
                .chatRoom(chatRoom)
                .sender(sender)
                .content(messageDto.getContent())
                .senderNickname(nickname)
                .imageCount(images.size())
                .createDate(now)
                .modifiedDate(now)
                .build();

        chatMessageRepository.save(chatMessage);

        imageService.saveChatImage(chatRoom.getId(), messageId, images, chatImagePath);

        String senderHash = getSenderHash(memberId);
        ChatMessageResponseDto responseDto = ChatMessageResponseDto.builder()
                .messageId(messageId)
                .roomId(chatRoom.getId())
                .senderNickname(nickname)
                .senderHash(senderHash)
                .content(messageDto.getContent())
                .imageCount(images.size())
                .createDate(now)
                .build();

        broadcastAndCache(chatRoom.getId(), responseDto);

        return responseDto;
    }

    private void broadcastAndCache(Long roomId, ChatMessageResponseDto responseDto) {
        messagingTemplate.convertAndSend("/sub/room/" + roomId, responseDto);

        try {
            String messageJson = objectMapper.writeValueAsString(responseDto);
            chatRedisService.saveMessageToCache(roomId, messageJson);
        } catch (JsonProcessingException e) {
            log.error("메시지 캐싱 중 직렬화 오류 발생: {}", responseDto, e);
        }
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

        // 멤버 권한 검증
        if (!chatRoomMemberRepository.existsByChatRoomAndMember(chatRoom, member)) {
            throw new MyException(MyErrorCode.NOT_CHATROOM_MEMBER);
        }

        List<ChatMessageResponseDto> messages = new ArrayList<>();
        List<String> cachedMessagesJson = chatRedisService.getRecentMessages(roomId);

        for (String messageJson : cachedMessagesJson) {
            try {
                messages.add(objectMapper.readValue(messageJson, ChatMessageResponseDto.class));
            } catch (JsonProcessingException e) {
                log.error("캐시 메시지 역직렬화 오류: {}", messageJson, e);
            }
        }

        messages.sort(Comparator.comparing(ChatMessageResponseDto::getCreateDate, Comparator.nullsLast(Comparator.naturalOrder())));

        // 캐시 부재 시 DB 조회
        if (messages.isEmpty()) {
            List<ChatMessage> dbMessages = chatMessageRepository.findTop50ByChatRoomOrderByCreateDateDesc(chatRoom);
            messages.addAll(dbMessages.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList()));
            messages.sort(Comparator.comparing(ChatMessageResponseDto::getCreateDate, Comparator.nullsLast(Comparator.naturalOrder())));
        }

        Long currentParticipants = chatRedisService.getRoomUserCount(roomId);
        String myHash = getSenderHash(memberId);

        return ChatRoomResponseDto.of(chatRoom, currentParticipants.intValue(), myHash, messages);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponseDto> getOlderMessages(Long roomId, Long memberId, Long lastId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        if (!chatRoomMemberRepository.existsByChatRoomAndMember(chatRoom, member)) {
            throw new MyException(MyErrorCode.NOT_CHATROOM_MEMBER);
        }

        // 과거 메시지 페이징 조회
        List<ChatMessage> olderMessages = chatMessageRepository.findTop50ByChatRoomAndIdLessThanOrderByIdDesc(chatRoom, lastId);

        return olderMessages.stream()
                .map(this::convertToDto)
                .sorted(Comparator.comparing(ChatMessageResponseDto::getCreateDate))
                .collect(Collectors.toList());
    }

    private ChatMessageResponseDto convertToDto(ChatMessage message) {
        return ChatMessageResponseDto.builder()
                .messageId(message.getId())
                .roomId(message.getChatRoom().getId())
                .senderNickname(message.getSenderNickname())
                .senderHash(getSenderHash(message.getSender().getId()))
                .content(message.getContent())
                .imageCount(message.getImageCount())
                .createDate(message.getCreateDate())
                .build();
    }

    @Transactional(readOnly = true)
    public List<PublicChatMessageResponseDto> getRecentTwoMessages(Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));

        Set<PublicChatMessageResponseDto> distinctMessages = new HashSet<>();

        // Redis 데이터 병합
        List<String> cachedMessagesJson = chatRedisService.getRecentMessages(roomId);
        for (String messageJson : cachedMessagesJson) {
            try {
                PublicChatMessageResponseDto dto = objectMapper.readValue(messageJson, PublicChatMessageResponseDto.class);
                if (dto.getMessageId() != null) {
                    distinctMessages.add(dto);
                }
            } catch (JsonProcessingException e) {
                log.error("Redis 메시지 역직렬화 오류: {}", messageJson, e);
            }
        }

        // DB 데이터 병합 및 중복 제거
        List<ChatMessage> messagesFromDb = chatMessageRepository.findTop2ByChatRoomOrderByCreateDateDesc(chatRoom);
        for (ChatMessage dbMessage : messagesFromDb) {
            distinctMessages.add(PublicChatMessageResponseDto.from(dbMessage));
        }

        // 최신순 필터링 후 오름차순 정렬
        return distinctMessages.stream()
                .sorted(Comparator.comparing(PublicChatMessageResponseDto::getCreateDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(2)
                .sorted(Comparator.comparing(PublicChatMessageResponseDto::getCreateDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }
}
