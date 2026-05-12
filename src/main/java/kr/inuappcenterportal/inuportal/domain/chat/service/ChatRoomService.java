package kr.inuappcenterportal.inuportal.domain.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatMessage;
import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatRoom;
import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatRoomMember;
import kr.inuappcenterportal.inuportal.domain.chat.enums.ChatMemberStatus;
import kr.inuappcenterportal.inuportal.domain.chat.enums.ChatRoomStatus;
import kr.inuappcenterportal.inuportal.domain.chat.enums.ChatRoomType;
import kr.inuappcenterportal.inuportal.domain.chat.dto.*;
import kr.inuappcenterportal.inuportal.domain.chat.repository.ChatMessageRepository;
import kr.inuappcenterportal.inuportal.domain.chat.repository.ChatRoomMemberRepository;
import kr.inuappcenterportal.inuportal.domain.chat.repository.ChatRoomRepository;
import kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.BlockRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.FriendRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;
    private final BlockRepository blockRepository;
    private final ChatRedisService chatRedisService;
    private final ChatBatchService chatBatchService;
    private final ImageService imageService;
    private final FriendRepository friendRepository;
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

        ChatRoom chatRoom = chatRoomRepository.findById(messageDto.getRoomId())
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));

        String nickname;
        if (chatRoom.isAnonymous()) {
            nickname = chatRedisService.getOrAssignAnonymousNickname(messageDto.getRoomId(), memberId);
        } else {
            nickname = sender.getNickname();
        }

        LocalDateTime now = LocalDateTime.now();
        String senderHash = getSenderHash(memberId);
        Long messageId = TSID.fast().toLong();

        int initialUnreadCount = (int) chatRoomMemberRepository.findAllByChatRoomAndStatus(chatRoom, ChatMemberStatus.JOINED).size() - 1;

        ChatMessageResponseDto responseDto = ChatMessageResponseDto.builder()
                .messageId(messageId)
                .roomId(messageDto.getRoomId())
                .senderNickname(nickname)
                .senderHash(senderHash)
                .content(messageDto.getContent())
                .imageCount(messageDto.getImageCount())
                .unreadCount(Math.max(0, initialUnreadCount))
                .createDate(now)
                .build();

        broadcastAndCache(messageDto.getRoomId(), responseDto);

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

        chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, sender)
                .ifPresent(m -> m.updateLastReadMessageId(messageId));
    }

    @Transactional
    public ChatMessageResponseDto sendMessageWithImages(ChatMessageRequestDto messageDto, List<MultipartFile> images, Long memberId) throws IOException {
        Member sender = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        ChatRoom chatRoom = chatRoomRepository.findById(messageDto.getRoomId())
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));

        String nickname = chatRoom.isAnonymous()
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

        int initialUnreadCount = (int) chatRoomMemberRepository.findAllByChatRoomAndStatus(chatRoom, ChatMemberStatus.JOINED).size() - 1;

        String senderHash = getSenderHash(memberId);
        ChatMessageResponseDto responseDto = ChatMessageResponseDto.builder()
                .messageId(messageId)
                .roomId(chatRoom.getId())
                .senderNickname(nickname)
                .senderHash(senderHash)
                .content(messageDto.getContent())
                .imageCount(images.size())
                .unreadCount(Math.max(0, initialUnreadCount))
                .createDate(now)
                .build();

        broadcastAndCache(chatRoom.getId(), responseDto);

        chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, sender)
                .ifPresent(m -> m.updateLastReadMessageId(messageId));

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

    private void broadcastReadUpdate(Long roomId) {
        messagingTemplate.convertAndSend("/sub/room/" + roomId + "/read", "updated");
    }

    @Transactional(readOnly = true)
    public UnreadTotalCountResponseDto getTotalUnreadCount(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        List<ChatRoomMember> joinedRooms = chatRoomMemberRepository.findAllByMemberAndStatus(member, ChatMemberStatus.JOINED);
        long totalUnread = 0;

        for (ChatRoomMember m : joinedRooms) {
            totalUnread += chatMessageRepository.countByChatRoomAndIdGreaterThan(m.getChatRoom(), m.getLastReadMessageId() == null ? 0L : m.getLastReadMessageId());
        }

        return new UnreadTotalCountResponseDto(totalUnread);
    }
    
    @Transactional(readOnly = true)
    public List<MyChatRoomResponseDto> getMyChatRooms(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        List<ChatRoomMember> joinedRooms = chatRoomMemberRepository.findAllByMemberAndStatus(member, ChatMemberStatus.JOINED);
        List<Long> blockedMemberIds = blockRepository.findAllByBlocker(member).stream()
                .map(b -> b.getBlocked().getId()).collect(Collectors.toList());

        return joinedRooms.stream().map(m -> {
            ChatRoom room = m.getChatRoom();
            
            Optional<ChatMessage> lastMsgOpt = chatMessageRepository.findTop50ByChatRoomOrderByCreateDateDesc(room).stream()
                    .filter(msg -> !blockedMemberIds.contains(msg.getSender().getId()))
                    .findFirst();

            long unreadCount = chatMessageRepository.countByChatRoomAndIdGreaterThan(room, m.getLastReadMessageId() == null ? 0L : m.getLastReadMessageId());

            String title = room.getTitle();
            String senderName = "";
            Long senderProfileImageNumber = null;
            LocalDateTime lastMessageTime = room.getCreateDate();
            String lastMessageContent = "아직 대화가 없습니다.";

            if (room.getType() == ChatRoomType.PERSONAL) {
                Optional<ChatRoomMember> otherMemberOpt = chatRoomMemberRepository.findAllByChatRoomAndStatus(room, ChatMemberStatus.JOINED)
                        .stream().filter(orm -> !orm.getMember().getId().equals(memberId)).findFirst();
                if (otherMemberOpt.isPresent()) {
                    title = otherMemberOpt.get().getMember().getNickname();
                }
            }

            if (lastMsgOpt.isPresent()) {
                ChatMessage lastMsg = lastMsgOpt.get();
                senderName = lastMsg.getSenderNickname();
                senderProfileImageNumber = lastMsg.getSender().getFireId();
                lastMessageTime = lastMsg.getCreateDate();
                lastMessageContent = lastMsg.getContent();
            }

            return MyChatRoomResponseDto.builder()
                    .roomId(room.getId())
                    .title(title)
                    .type(room.getType())
                    .lastMessage(lastMessageContent)
                    .lastMessageTime(lastMessageTime)
                    .unreadCount(unreadCount)
                    .senderName(senderName)
                    .senderProfileImageNumber(senderProfileImageNumber)
                    .isOwner(room.getCreator().getId().equals(memberId))
                    .isOfficial(room.isOfficial())
                    .build();
        })
        .sorted(Comparator.comparing(MyChatRoomResponseDto::getLastMessageTime, Comparator.reverseOrder()))
        .collect(Collectors.toList());
    }


    @Transactional
    public ChatRoomResponseDto getOrCreatePersonalChatRoom(PersonalChatRoomRequestDto requestDto, Long memberId) {
        Member requester = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        List<Long> targetIds = requestDto.getTargetMemberIds();
        if (targetIds == null || targetIds.isEmpty()) {
            throw new MyException(MyErrorCode.EMPTY_REQUEST);
        }

        if (targetIds.contains(memberId)) {
            throw new MyException(MyErrorCode.NOT_SELF_CHAT); // 자기 자신과는 채팅방을 만들 수 없음
        }

        Set<Long> allMemberIds = new HashSet<>(targetIds);
        allMemberIds.add(memberId);

        boolean isOfficial = false;
        if (requestDto.isAdminMode()) {
            if (!requester.getRoles().contains("ROLE_ADMIN")) {
                throw new MyException(MyErrorCode.NOT_ADMIN);
            }
            isOfficial = true;
        }

        // 기존 채팅방 확인
        List<ChatRoomMember> myJoinedRooms = chatRoomMemberRepository.findAllByMemberAndStatus(requester, ChatMemberStatus.JOINED);
        for (ChatRoomMember m : myJoinedRooms) {
            ChatRoom room = m.getChatRoom();
            if (room.getType() == ChatRoomType.PERSONAL && room.isOfficial() == isOfficial) {
                List<ChatRoomMember> roomMembers = chatRoomMemberRepository.findAllByChatRoomAndStatus(room, ChatMemberStatus.JOINED);
                Set<Long> roomMemberIds = roomMembers.stream().map(cm -> cm.getMember().getId()).collect(Collectors.toSet());
                if (roomMemberIds.equals(allMemberIds)) {
                    Long currentParticipants = chatRedisService.getRoomUserCount(room.getId());
                    boolean isOwner = room.getCreator().getId().equals(memberId);
                    return ChatRoomResponseDto.of(room, currentParticipants.intValue(), getSenderHash(memberId), isOwner);
                }
            }
        }

        // 친구 여부 확인 (공식 모드 아닐 때만)
        if (!isOfficial) {
            for (Long targetId : targetIds) {
                Member target = memberRepository.findById(targetId)
                        .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
                boolean isFriend = friendRepository.existsByRequesterAndReceiverAndStatus(requester, target, FriendStatus.ACCEPTED) ||
                                   friendRepository.existsByRequesterAndReceiverAndStatus(target, requester, FriendStatus.ACCEPTED);
                if (!isFriend) {
                    throw new MyException(MyErrorCode.NOT_FRIEND);
                }
            }
        }

        // 새 채팅방 생성
        String title = (allMemberIds.size() == 2) ? "" : "그룹 채팅";
        if (isOfficial) {
            title = "운영자 공식 메시지";
        }

        ChatRoom chatRoom = ChatRoom.builder()
                .title(title)
                .maxCapacity(100) // 개인톡/단톡은 넉넉하게
                .isAnonymous(false)
                .type(ChatRoomType.PERSONAL)
                .creator(requester)
                .isOfficial(isOfficial)
                .build();
        chatRoomRepository.save(chatRoom);

        for (Long id : allMemberIds) {
            Member member = memberRepository.findById(id).orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
            ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                    .chatRoom(chatRoom)
                    .member(member)
                    .build();
            chatRoomMemberRepository.save(chatRoomMember);
            chatRedisService.addUserToRoom(chatRoom.getId(), id);
        }

        return ChatRoomResponseDto.of(chatRoom, allMemberIds.size(), getSenderHash(memberId), true);
    }

    @Transactional
    public ChatRoomResponseDto createChatRoom(ChatRoomCreateRequestDto requestDto, Long memberId) {
        Member creator = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        boolean isAnonymous = requestDto.getType() == ChatRoomType.PERSONAL ? false : requestDto.getIsAnonymous();

        ChatRoom chatRoom = ChatRoom.builder()
                .title(requestDto.getTitle())
                .maxCapacity(requestDto.getMaxCapacity())
                .isAnonymous(isAnonymous)
                .type(requestDto.getType())
                .creator(creator)
                .isOfficial(false)
                .build();
        chatRoomRepository.save(chatRoom);

        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .member(creator)
                .build();
        chatRoomMemberRepository.save(chatRoomMember);

        chatRedisService.addUserToRoom(chatRoom.getId(), memberId);

        return ChatRoomResponseDto.of(chatRoom, 1, getSenderHash(memberId), true);
    }

    @Transactional
    public ChatRoomResponseDto joinChatRoom(Long roomId, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));

        if (chatRoom.getStatus() == ChatRoomStatus.CLOSED) {
            throw new MyException(MyErrorCode.CHATROOM_CLOSED);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        Long currentParticipants = chatRedisService.getRoomUserCount(roomId);

        Optional<ChatRoomMember> existingMember = chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, member);
        if (existingMember.isPresent()) {
            ChatRoomMember m = existingMember.get();
            if (m.getStatus() == ChatMemberStatus.LEFT) {
                m.rejoin();
                chatRedisService.addUserToRoom(roomId, memberId);
            }
            boolean isOwner = chatRoom.getCreator().getId().equals(memberId);
            return ChatRoomResponseDto.of(chatRoom, currentParticipants.intValue(), getSenderHash(memberId), isOwner);
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

        boolean isOwner = chatRoom.getCreator().getId().equals(memberId);
        return ChatRoomResponseDto.of(chatRoom, currentParticipants.intValue() + 1, getSenderHash(memberId), isOwner);
    }

    @Transactional
    public void leaveChatRoom(Long roomId, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        ChatRoomMember chatRoomMember = chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, member)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_CHATROOM_MEMBER));

        chatRoomMember.leave();
        chatRedisService.removeUserFromRoom(roomId, memberId);
    }

    @Transactional
    public void closeChatRoom(Long roomId, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));

        if (!chatRoom.getCreator().getId().equals(memberId)) {
            throw new MyException(MyErrorCode.NOT_CHATROOM_OWNER);
        }

        chatRoom.close();
    }

    @Transactional(readOnly = true)
    public List<ChatRoomMemberResponseDto> getParticipants(Long roomId, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));

        List<ChatRoomMember> members = chatRoomMemberRepository.findAllByChatRoomAndStatus(chatRoom, ChatMemberStatus.JOINED);

        return members.stream().map(m -> {
            Member member = m.getMember();
            String nickname;
            if (chatRoom.isAnonymous()) {
                nickname = chatRedisService.getOrAssignAnonymousNickname(roomId, member.getId());
            } else {
                nickname = member.getNickname();
            }

            return ChatRoomMemberResponseDto.builder()
                    .nickname(nickname)
                    .studentId(chatRoom.isAnonymous() ? null : member.getStudentId())
                    .fireId(chatRoom.isAnonymous() ? null : member.getFireId())
                    .isMe(member.getId().equals(memberId))
                    .isOwner(member.getId().equals(chatRoom.getCreator().getId()))
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public ChatRoomResponseDto getChatRoomMessages(Long roomId, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        ChatRoomMember chatRoomMember = chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, member)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_CHATROOM_MEMBER));

        if (chatRoomMember.getStatus() == ChatMemberStatus.LEFT) {
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

        if (messages.isEmpty()) {
            List<ChatMessage> dbMessages = chatMessageRepository.findTop50ByChatRoomOrderByCreateDateDesc(chatRoom);
            messages.addAll(dbMessages.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList()));
            messages.sort(Comparator.comparing(ChatMessageResponseDto::getCreateDate, Comparator.nullsLast(Comparator.naturalOrder())));
        }

        List<Long> readIds = chatRoomMemberRepository.findAllByChatRoomAndStatus(chatRoom, ChatMemberStatus.JOINED)
                .stream().map(m -> m.getLastReadMessageId() == null ? 0L : m.getLastReadMessageId()).collect(Collectors.toList());

        messages = messages.stream().map(msg -> {
            int unread = (int) readIds.stream().filter(lastRead -> lastRead < msg.getMessageId()).count();
            return ChatMessageResponseDto.builder()
                    .messageId(msg.getMessageId())
                    .roomId(msg.getRoomId())
                    .senderNickname(msg.getSenderNickname())
                    .senderHash(msg.getSenderHash())
                    .content(msg.getContent())
                    .imageCount(msg.getImageCount())
                    .unreadCount(unread)
                    .createDate(msg.getCreateDate())
                    .build();
        }).collect(Collectors.toList());

        if (!messages.isEmpty()) {
            Long lastMessageId = messages.get(messages.size() - 1).getMessageId();
            if (chatRoomMember.getLastReadMessageId() == null || lastMessageId > chatRoomMember.getLastReadMessageId()) {
                chatRoomMember.updateLastReadMessageId(lastMessageId);
                broadcastReadUpdate(roomId);
            }
        }

        Long currentParticipants = chatRedisService.getRoomUserCount(roomId);
        String myHash = getSenderHash(memberId);
        boolean isOwner = chatRoom.getCreator().getId().equals(memberId);

        return ChatRoomResponseDto.of(chatRoom, currentParticipants.intValue(), myHash, isOwner, messages);
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

        List<ChatMessage> olderMessages = chatMessageRepository.findTop50ByChatRoomAndIdLessThanOrderByIdDesc(chatRoom, lastId);
        List<Long> readIds = chatRoomMemberRepository.findAllByChatRoomAndStatus(chatRoom, ChatMemberStatus.JOINED)
                .stream().map(m -> m.getLastReadMessageId() == null ? 0L : m.getLastReadMessageId()).collect(Collectors.toList());

        return olderMessages.stream()
                .map(msg -> {
                    ChatMessageResponseDto dto = convertToDto(msg);
                    int unread = (int) readIds.stream().filter(lastRead -> lastRead < dto.getMessageId()).count();
                    return ChatMessageResponseDto.builder()
                            .messageId(dto.getMessageId())
                            .roomId(dto.getRoomId())
                            .senderNickname(dto.getSenderNickname())
                            .senderHash(dto.getSenderHash())
                            .content(dto.getContent())
                            .imageCount(dto.getImageCount())
                            .unreadCount(unread)
                            .createDate(dto.getCreateDate())
                            .build();
                })
                .sorted(Comparator.comparing(ChatMessageResponseDto::getCreateDate))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PublicChatMessageResponseDto> getPublicMessages(Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));

        if (chatRoom.getType() != ChatRoomType.OPEN) {
            throw new MyException(MyErrorCode.HAS_NOT_POST_AUTHORIZATION); // 오픈채팅이 아니면 접근 불가
        }

        List<ChatMessage> messages = chatMessageRepository.findTop2ByChatRoomOrderByCreateDateDesc(chatRoom);
        return messages.stream()
                .map(PublicChatMessageResponseDto::from)
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
                .unreadCount(0)
                .createDate(message.getCreateDate())
                .build();
    }
}
