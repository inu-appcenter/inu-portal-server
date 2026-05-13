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
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmAsyncService;
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
    private final FcmAsyncService fcmAsyncService;
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
        if (chatRoom.isOfficial() && sender.getRoles().contains("ROLE_ADMIN")) {
            nickname = chatRedisService.getOrAssignAdminNickname(messageDto.getRoomId(), memberId);
        } else if (chatRoom.isAnonymous()) {
            nickname = chatRedisService.getOrAssignAnonymousNickname(messageDto.getRoomId(), memberId);
        } else {
            nickname = sender.getNickname();
        }

        LocalDateTime now = LocalDateTime.now();
        String senderHash = getSenderHash(memberId);
        Long messageId = TSID.fast().toLong();

        Set<String> activeUserIds = chatRedisService.getRoomUserIds(messageDto.getRoomId());
        int totalJoined = chatRoomMemberRepository.countByChatRoomAndStatus(chatRoom, ChatMemberStatus.JOINED);
        int initialUnreadCount = Math.max(0, totalJoined - activeUserIds.size());

        ChatMessageResponseDto responseDto = ChatMessageResponseDto.builder()
                .messageId(messageId)
                .roomId(messageDto.getRoomId())
                .senderNickname(nickname)
                .senderHash(senderHash)
                .content(messageDto.getContent())
                .imageCount(messageDto.getImageCount())
                .unreadCount(initialUnreadCount)
                .createDate(now)
                .build();

        broadcastAndCache(messageDto.getRoomId(), responseDto);

        // 현재 방에 있는 사람들의 읽음 상태를 DB에 일괄 업데이트
        List<Long> activeMemberIds = activeUserIds.stream().map(Long::parseLong).toList();
        chatRoomMemberRepository.updateLastReadMessageIdByRoomAndMemberIds(chatRoom, activeMemberIds, messageId);

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

        sendChatNotification(chatRoom, sender, nickname, messageDto.getContent());
    }

    @Transactional
    public ChatMessageResponseDto sendMessageWithImages(ChatMessageRequestDto messageDto, List<MultipartFile> images,
            Long memberId) throws IOException {
        Member sender = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        ChatRoom chatRoom = chatRoomRepository.findById(messageDto.getRoomId())
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));

        String nickname;
        if (chatRoom.isOfficial() && sender.getRoles().contains("ROLE_ADMIN")) {
            nickname = chatRedisService.getOrAssignAdminNickname(chatRoom.getId(), memberId);
        } else if (chatRoom.isAnonymous()) {
            nickname = chatRedisService.getOrAssignAnonymousNickname(chatRoom.getId(), memberId);
        } else {
            nickname = sender.getNickname();
        }

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

        Set<String> activeUserIds = chatRedisService.getRoomUserIds(chatRoom.getId());
        int totalJoined = chatRoomMemberRepository.countByChatRoomAndStatus(chatRoom, ChatMemberStatus.JOINED);
        int initialUnreadCount = Math.max(0, totalJoined - activeUserIds.size());

        String senderHash = getSenderHash(memberId);
        ChatMessageResponseDto responseDto = ChatMessageResponseDto.builder()
                .messageId(messageId)
                .roomId(chatRoom.getId())
                .senderNickname(nickname)
                .senderHash(senderHash)
                .content(messageDto.getContent())
                .imageCount(images.size())
                .unreadCount(initialUnreadCount)
                .createDate(now)
                .build();

        broadcastAndCache(chatRoom.getId(), responseDto);

        // 현재 방에 있는 사람들의 읽음 상태를 DB에 일괄 업데이트
        List<Long> activeMemberIds = activeUserIds.stream().map(Long::parseLong).toList();
        chatRoomMemberRepository.updateLastReadMessageIdByRoomAndMemberIds(chatRoom, activeMemberIds, messageId);

        sendChatNotification(chatRoom, sender, nickname, images.isEmpty() ? messageDto.getContent() : "사진을 보냈습니다.");

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

        List<ChatRoomMember> joinedRooms = chatRoomMemberRepository.findAllByMemberAndStatus(member,
                ChatMemberStatus.JOINED);
        long totalUnread = 0;

        for (ChatRoomMember m : joinedRooms) {
            totalUnread += chatMessageRepository.countByChatRoomAndIdGreaterThan(m.getChatRoom(),
                    m.getLastReadMessageId() == null ? 0L : m.getLastReadMessageId());
        }

        return new UnreadTotalCountResponseDto(totalUnread);
    }

    @Transactional(readOnly = true)
    public List<MyChatRoomResponseDto> getMyChatRooms(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        List<ChatRoomMember> joinedRooms = chatRoomMemberRepository.findAllByMemberAndStatus(member,
                ChatMemberStatus.JOINED);
        List<Long> blockedMemberIds = blockRepository.findAllByBlocker(member).stream()
                .map(b -> b.getBlocked().getId()).collect(Collectors.toList());

        return joinedRooms.stream()
                .filter(m -> m.getChatRoom().getStatus() == ChatRoomStatus.ACTIVE)
                .map(m -> {
                    ChatRoom room = m.getChatRoom();

                    Optional<ChatMessage> lastMsgOpt = chatMessageRepository
                            .findTop50ByChatRoomOrderByCreateDateDesc(room)
                            .stream()
                            .filter(msg -> !blockedMemberIds.contains(msg.getSender().getId()))
                            .findFirst();

                    long unreadCount = chatMessageRepository.countByChatRoomAndIdGreaterThan(room,
                            m.getLastReadMessageId() == null ? 0L : m.getLastReadMessageId());
                    // long currentParticipants = chatRedisService.getRoomUserCount(room.getId());

                    String title = room.getTitle();
                    String senderName = "";
                    Long senderProfileImageNumber = null;
                    LocalDateTime lastMessageTime = room.getCreateDate();
                    String lastMessageContent = "아직 대화가 없습니다.";

                    // 개인 채팅방 처리
                    if (room.getType() == ChatRoomType.PERSONAL) {
                        if (room.isOfficial() && !member.getRoles().contains("ROLE_ADMIN")) {
                            title = "INTIP 운영자";
                        } else {
                            List<ChatRoomMember> roomMembers = chatRoomMemberRepository.findAllByChatRoomAndStatus(room,
                                    ChatMemberStatus.JOINED);
                            // 1:1 채팅방(참여 인원 2명)인 경우 항상 상대방 닉네임 사용
                            if (roomMembers.size() == 2) {
                                Optional<ChatRoomMember> otherMemberOpt = roomMembers.stream()
                                        .filter(orm -> !orm.getMember().getId().equals(memberId)).findFirst();
                                title = otherMemberOpt.map(chatRoomMember -> chatRoomMember.getMember().getNickname())
                                        .orElse("알 수 없음");
                            }
                            // 단체방인데 제목이 없는 경우만 상대방 닉네임 하나 노출 (또는 기본값 유지)
                            else if (title == null || title.isEmpty()) {
                                title = roomMembers.stream()
                                        .filter(orm -> !orm.getMember().getId().equals(memberId))
                                        .findFirst()
                                        .map(crm -> crm.getMember().getNickname())
                                        .orElse("알 수 없음");
                            }
                        }
                    }

                    if (lastMsgOpt.isPresent()) {
                        ChatMessage lastMsg = lastMsgOpt.get();
                        senderName = lastMsg.getSenderNickname();
                        senderProfileImageNumber = lastMsg.getSender().getFireId();
                        lastMessageTime = lastMsg.getCreateDate();
                        lastMessageContent = lastMsg.getContent();
                    }

                    int memberCount = chatRoomMemberRepository.countByChatRoomAndStatus(room, ChatMemberStatus.JOINED);

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
                            .currentParticipants(memberCount)
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

        // 차단 여부 확인 (개인 채팅인 경우)
        for (Long targetId : targetIds) {
            Member target = memberRepository.findById(targetId)
                    .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
            if (blockRepository.existsByBlockerAndBlocked(requester, target) ||
                    blockRepository.existsByBlockerAndBlocked(target, requester)) {
                throw new MyException(MyErrorCode.USER_NOT_FOUND); // 검색과 동일하게 USER_NOT_FOUND로 응답
            }
        }

        boolean isOfficial = false;
        if (requestDto.isAdminMode()) {
            if (!requester.getRoles().contains("ROLE_ADMIN")) {
                throw new MyException(MyErrorCode.NOT_ADMIN);
            }
            // 운영자 채팅 대상 확인 (운영자끼리는 불가능)
            for (Long targetId : targetIds) {
                Member target = memberRepository.findById(targetId)
                        .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
                if (target.getRoles().contains("ROLE_ADMIN")) {
                    throw new MyException(MyErrorCode.INVALID_OFFICIAL_CHAT_TARGET);
                }
            }
            isOfficial = true;
        }

        Set<Long> allMemberIds = new HashSet<>(targetIds);
        allMemberIds.add(memberId);

        // 기존 채팅방 확인
        if (isOfficial) {
            // 공식 상담방인 경우, 대상 학생이 포함된 기존 공식 방이 있는지 확인 (통합 상담방 모델)
            for (Long targetId : targetIds) {
                Member target = memberRepository.findById(targetId)
                        .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
                List<ChatRoomMember> targetJoinedRooms = chatRoomMemberRepository.findAllByMemberAndStatus(target,
                        ChatMemberStatus.JOINED);
                for (ChatRoomMember tm : targetJoinedRooms) {
                    ChatRoom room = tm.getChatRoom();
                    if (room.getType() == ChatRoomType.PERSONAL && room.isOfficial()) {
                        // 이미 관리자가 참여 중인지 확인
                        if (!chatRoomMemberRepository.existsByChatRoomAndMember(room, requester)) {
                            ChatRoomMember newAdminMember = ChatRoomMember.builder()
                                    .chatRoom(room)
                                    .member(requester)
                                    .build();
                            chatRoomMemberRepository.save(newAdminMember);
                        }
                        Long currentParticipants = (long) chatRoomMemberRepository.countByChatRoomAndStatus(room,
                                ChatMemberStatus.JOINED);
                        boolean isOwner = room.getCreator().getId().equals(memberId);
                        return ChatRoomResponseDto.of(room, currentParticipants.intValue(), getSenderHash(memberId),
                                isOwner);
                    }
                }
            }
        } else {
            // 일반 개인 채팅방인 경우, 정확히 멤버가 일치하는 방 찾기
            List<ChatRoomMember> myJoinedRooms = chatRoomMemberRepository.findAllByMemberAndStatus(requester,
                    ChatMemberStatus.JOINED);
            for (ChatRoomMember m : myJoinedRooms) {
                ChatRoom room = m.getChatRoom();
                if (room.getType() == ChatRoomType.PERSONAL && !room.isOfficial()) {
                    List<ChatRoomMember> roomMembers = chatRoomMemberRepository.findAllByChatRoomAndStatus(room,
                            ChatMemberStatus.JOINED);
                    Set<Long> roomMemberIds = roomMembers.stream().map(cm -> cm.getMember().getId())
                            .collect(Collectors.toSet());
                    if (roomMemberIds.equals(allMemberIds)) {
                        int memberCount = chatRoomMemberRepository.countByChatRoomAndStatus(room,
                                ChatMemberStatus.JOINED);
                        boolean isOwner = room.getCreator().getId().equals(memberId);
                        return ChatRoomResponseDto.of(room, memberCount, getSenderHash(memberId),
                                isOwner);
                    }
                }
            }
        }

        // 친구 여부 확인 (공식 모드 아닐 때만)
        if (!isOfficial) {
            for (Long targetId : targetIds) {
                Member target = memberRepository.findById(targetId)
                        .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
                boolean isFriend = friendRepository.existsByRequesterAndReceiverAndStatus(requester, target,
                        FriendStatus.ACCEPTED) ||
                        friendRepository.existsByRequesterAndReceiverAndStatus(target, requester,
                                FriendStatus.ACCEPTED);
                if (!isFriend) {
                    throw new MyException(MyErrorCode.NOT_FRIEND);
                }
            }
        }

        // 새 채팅방 생성
        String title = requestDto.getTitle();
        if (isOfficial) {
            title = "INTIP 운영자";
        } else if (title == null || title.trim().isEmpty()) {
            title = (allMemberIds.size() == 2) ? "" : "그룹 채팅";
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
            Member member = memberRepository.findById(id)
                    .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
            ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                    .chatRoom(chatRoom)
                    .member(member)
                    .build();
            chatRoomMemberRepository.save(chatRoomMember);
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

        Optional<ChatRoomMember> existingMember = chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, member);
        if (existingMember.isPresent()) {
            ChatRoomMember m = existingMember.get();
            if (m.getStatus() == ChatMemberStatus.LEFT) {
                m.rejoin();
            }
            int memberCount = chatRoomMemberRepository.countByChatRoomAndStatus(chatRoom, ChatMemberStatus.JOINED);
            boolean isOwner = chatRoom.getCreator().getId().equals(memberId);
            return ChatRoomResponseDto.of(chatRoom, memberCount, getSenderHash(memberId), isOwner);
        }

        int totalParticipants = chatRoomMemberRepository.countByChatRoomAndStatus(chatRoom, ChatMemberStatus.JOINED);
        if (totalParticipants >= chatRoom.getMaxCapacity()) {
            throw new MyException(MyErrorCode.CHATROOM_FULL);
        }

        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .member(member)
                .build();
        chatRoomMemberRepository.save(chatRoomMember);

        boolean isOwner = chatRoom.getCreator().getId().equals(memberId);
        int finalParticipants = chatRoomMemberRepository.countByChatRoomAndStatus(chatRoom, ChatMemberStatus.JOINED);
        return ChatRoomResponseDto.of(chatRoom, finalParticipants, getSenderHash(memberId), isOwner);
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

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        // 방장이거나 관리자인지 확인
        boolean isAdmin = member.getRoles().contains("ROLE_ADMIN");
        if (!chatRoom.getCreator().getId().equals(memberId) && !isAdmin) {
            throw new MyException(MyErrorCode.NOT_CHATROOM_OWNER);
        }

        if (chatRoom.getType() != ChatRoomType.OPEN) {
            throw new MyException(MyErrorCode.HAS_NOT_POST_AUTHORIZATION);
        }

        chatRoom.close();
    }

    @Transactional(readOnly = true)
    public List<ChatRoomMemberResponseDto> getParticipants(Long roomId, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));

        List<ChatRoomMember> members = chatRoomMemberRepository.findAllByChatRoomAndStatus(chatRoom,
                ChatMemberStatus.JOINED);

        return members.stream().map(m -> {
            Member member = m.getMember();
            String nickname;
            if (chatRoom.isOfficial() && member.getRoles().contains("ROLE_ADMIN")) {
                nickname = chatRedisService.getOrAssignAdminNickname(roomId, member.getId());
            } else if (chatRoom.isAnonymous()) {
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

        messages.sort(Comparator.comparing(ChatMessageResponseDto::getCreateDate,
                Comparator.nullsLast(Comparator.naturalOrder())));

        if (messages.isEmpty()) {
            List<ChatMessage> dbMessages = chatMessageRepository.findTop50ByChatRoomOrderByCreateDateDesc(chatRoom);
            messages.addAll(dbMessages.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList()));
            messages.sort(Comparator.comparing(ChatMessageResponseDto::getCreateDate,
                    Comparator.nullsLast(Comparator.naturalOrder())));
        }

        List<Long> blockedMemberIds = blockRepository.findAllByBlocker(member).stream()
                .map(b -> b.getBlocked().getId()).collect(Collectors.toList());
        Set<String> blockedHashes = blockedMemberIds.stream()
                .map(this::getSenderHash)
                .collect(Collectors.toSet());

        List<Long> readIds = chatRoomMemberRepository.findAllByChatRoomAndStatus(chatRoom, ChatMemberStatus.JOINED)
                .stream().map(m -> m.getLastReadMessageId() == null ? 0L : m.getLastReadMessageId())
                .collect(Collectors.toList());

        messages = messages.stream()
                .filter(msg -> msg.getSenderHash() == null || !blockedHashes.contains(msg.getSenderHash()))
                .map(msg -> {
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
            if (chatRoomMember.getLastReadMessageId() == null
                    || lastMessageId > chatRoomMember.getLastReadMessageId()) {
                chatRoomMember.updateLastReadMessageId(lastMessageId);
                broadcastReadUpdate(roomId);
            }
        }

        int memberCount = chatRoomMemberRepository.countByChatRoomAndStatus(chatRoom, ChatMemberStatus.JOINED);
        boolean isOwner = chatRoom.getCreator().getId().equals(memberId);

        String title = chatRoom.getTitle();
        if (chatRoom.getType() == ChatRoomType.PERSONAL) {
            if (chatRoom.isOfficial() && !member.getRoles().contains("ROLE_ADMIN")) {
                title = "INTIP 운영자";
            } else {
                List<ChatRoomMember> members = chatRoomMemberRepository.findAllByChatRoomAndStatus(chatRoom,
                        ChatMemberStatus.JOINED);
                if (members.size() == 2) {
                    title = members.stream()
                            .filter(m -> !m.getMember().getId().equals(memberId))
                            .findFirst()
                            .map(crm -> crm.getMember().getNickname())
                            .orElse("알 수 없음");
                }
            }
        }

        return ChatRoomResponseDto.builder()
                .id(chatRoom.getId())
                .title(title)
                .maxCapacity(chatRoom.getMaxCapacity())
                .isAnonymous(chatRoom.isAnonymous())
                .type(chatRoom.getType())
                .status(chatRoom.getStatus())
                .currentParticipants(memberCount)
                .createDate(chatRoom.getCreateDate())
                .myHash(getSenderHash(memberId))
                .isOwner(isOwner)
                .isOfficial(chatRoom.isOfficial())
                .messages(messages)
                .build();
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

        List<ChatMessage> olderMessages = chatMessageRepository.findTop50ByChatRoomAndIdLessThanOrderByIdDesc(chatRoom,
                lastId);
        List<Long> readIds = chatRoomMemberRepository.findAllByChatRoomAndStatus(chatRoom, ChatMemberStatus.JOINED)
                .stream().map(m -> m.getLastReadMessageId() == null ? 0L : m.getLastReadMessageId())
                .collect(Collectors.toList());

        List<Long> blockedMemberIds = blockRepository.findAllByBlocker(member).stream()
                .map(b -> b.getBlocked().getId()).collect(Collectors.toList());
        Set<String> blockedHashes = blockedMemberIds.stream()
                .map(this::getSenderHash)
                .collect(Collectors.toSet());

        return olderMessages.stream()
                .map(this::convertToDto)
                .filter(dto -> dto.getSenderHash() == null || !blockedHashes.contains(dto.getSenderHash()))
                .map(dto -> {
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

        List<ChatMessage> messages = chatMessageRepository.findTop50ByChatRoomOrderByCreateDateDesc(chatRoom);
        return messages.stream()
                .map(PublicChatMessageResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateChatRoomTitle(Long roomId, ChatRoomTitleUpdateRequestDto requestDto, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        // 1. 채팅방 멤버인지 확인
        if (!chatRoomMemberRepository.existsByChatRoomAndMember(chatRoom, member)) {
            throw new MyException(MyErrorCode.NOT_CHATROOM_MEMBER);
        }

        // 2. 오픈채팅인 경우 방장인지 확인 (관리자는 허용)
        if (chatRoom.getType() == ChatRoomType.OPEN) {
            boolean isAdmin = member.getRoles().contains("ROLE_ADMIN");
            if (!chatRoom.getCreator().getId().equals(memberId) && !isAdmin) {
                throw new MyException(MyErrorCode.NOT_CHATROOM_OWNER);
            }
        } else if (chatRoom.getType() == ChatRoomType.PERSONAL) {
            // 1:1 채팅방은 이름 변경 불가
            if (chatRoomMemberRepository.countByChatRoomAndStatus(chatRoom, ChatMemberStatus.JOINED) == 2) {
                throw new MyException(MyErrorCode.HAS_NOT_POST_AUTHORIZATION);
            }
        }

        chatRoom.updateTitle(requestDto.getTitle());
    }

    @Transactional
    public void enterChatRoom(Long roomId, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        // 1. Redis에 접속 정보 추가
        chatRedisService.addUserToRoom(roomId, memberId);

        // 2. 안 읽은 메시지 읽음 처리 (마지막 메시지 ID로 갱신)
        ChatRoomMember chatRoomMember = chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, member)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_CHATROOM_MEMBER));

        chatMessageRepository.findTopByChatRoomOrderByCreateDateDesc(chatRoom).ifPresent(lastMsg -> {
            chatRoomMember.updateLastReadMessageId(lastMsg.getId());
        });

        // 3. 다른 참여자들에게 '상태 업데이트(읽음)' 신호 전송
        messagingTemplate.convertAndSend("/sub/room/" + roomId, "updated");
    }

    @Transactional
    public void exitChatRoom(Long roomId, Long memberId) {
        // Redis에서 접속 정보 제거
        chatRedisService.removeUserFromRoom(roomId, memberId);
    }

    private void sendChatNotification(ChatRoom room, Member sender, String senderNickname, String content) {
        // 1. 현재 방에 접속 중인 사용자 ID 목록 가져오기
        Set<String> activeUserIds = chatRedisService.getRoomUserIds(room.getId());

        // 2. 알림을 받을 멤버 필터링 (참여 중인 멤버 - 나 - 현재 접속자)
        List<ChatRoomMember> joinedMembers = chatRoomMemberRepository.findAllByChatRoomAndStatus(room,
                ChatMemberStatus.JOINED);
        List<Long> targetMemberIds = joinedMembers.stream()
                .map(ChatRoomMember::getMember)
                .filter(m -> !m.getId().equals(sender.getId())) // 발신자 제외
                .filter(m -> !activeUserIds.contains(String.valueOf(m.getId()))) // 현재 접속자 제외
                .map(Member::getId)
                .toList();

        if (targetMemberIds.isEmpty()) {
            return;
        }

        // 3. 알림 제목 및 내용 구성
        String title;
        String body;

        if (room.isOfficial()) {
            title = "INTIP 운영자";
            body = content;
        } else if (room.getType() == ChatRoomType.PERSONAL) {
            if (joinedMembers.size() == 2) {
                // 1:1 채팅
                title = senderNickname;
                body = content;
            } else {
                // 그룹 채팅
                title = (room.getTitle() == null || room.getTitle().isEmpty()) ? "그룹 채팅" : room.getTitle();
                body = senderNickname + ": " + content;
            }
        } else {
            // 오픈 채팅 (익명 포함)
            title = room.getTitle();
            body = senderNickname + ": " + content;
        }

        // 4. 알림 전송 (비동기 처리됨)
        // 채팅 알림도 이력에 남기기 위해 sendToMembers (또는 이력을 남기지 않으려면 sendUntrackedNotification)
        // 사용 가능
        // 사용자가 알림 이력 조회를 원하므로 sendKeywordNotice 스타일의 배치를 활용하거나 간단한 래퍼를 사용
        fcmAsyncService.sendAsyncUntrackedNotification(targetMemberIds, title, body);
    }

    private ChatMessageResponseDto convertToDto(ChatMessage message) {
        String nickname = message.getSenderNickname();
        if (message.getChatRoom().isOfficial() && message.getSender().getRoles().contains("ROLE_ADMIN")) {
            nickname = chatRedisService.getOrAssignAdminNickname(message.getChatRoom().getId(),
                    message.getSender().getId());
        }

        return ChatMessageResponseDto.builder()
                .messageId(message.getId())
                .roomId(message.getChatRoom().getId())
                .senderNickname(nickname)
                .senderHash(getSenderHash(message.getSender().getId()))
                .content(message.getContent())
                .imageCount(message.getImageCount())
                .unreadCount(0)
                .createDate(message.getCreateDate())
                .build();
    }
}
