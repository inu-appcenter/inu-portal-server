package kr.inuappcenterportal.inuportal.domain.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatMessage;
import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatRoom;
import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatRoomMember;
import kr.inuappcenterportal.inuportal.domain.chat.enums.ChatMemberStatus;
import kr.inuappcenterportal.inuportal.domain.chat.enums.ChatRoomStatus;
import kr.inuappcenterportal.inuportal.domain.chat.enums.ChatRoomType;
import kr.inuappcenterportal.inuportal.domain.chat.enums.MessageType;
import kr.inuappcenterportal.inuportal.domain.chat.dto.*;
import kr.inuappcenterportal.inuportal.domain.chat.repository.ChatMessageRepository;
import kr.inuappcenterportal.inuportal.domain.chat.repository.ChatRoomMemberRepository;
import kr.inuappcenterportal.inuportal.domain.chat.repository.ChatRoomRepository;
import kr.inuappcenterportal.inuportal.domain.member.dto.MemberProfileResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.model.Friend;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
    private final InuChatAiService inuChatAiService;

    @Value("${jwtSecret}")
    private String salt;

    @Value("${chatImagePath}")
    private String chatImagePath;

    @Value("${imagePath}")
    private String imagePath;

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

        ChatRoomMember senderChatRoomMember = chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, sender)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_CHATROOM_MEMBER));

        LocalDateTime now = LocalDateTime.now();
        String senderHash = getSenderHash(memberId);
        Long messageId = TSID.fast().toLong();

        Set<String> activeUserIds = chatRedisService.getRoomUserIds(messageDto.getRoomId());
        int totalJoined = chatRoomMemberRepository.countByChatRoomAndStatus(chatRoom, ChatMemberStatus.JOINED);
        int initialUnreadCount = Math.max(0, totalJoined - activeUserIds.size());

        MessageType messageType = messageDto.getMessageType() != null ? messageDto.getMessageType() : MessageType.TEXT;
        boolean isBotQuestion = messageType == MessageType.BOT_QUESTION
                || (messageDto.getContent() != null && messageDto.getContent().contains("[CHATBULI_QUESTION]"));
        if (isBotQuestion) {
            messageType = MessageType.BOT_QUESTION;
        }

        ChatMessageResponseDto responseDto = ChatMessageResponseDto.builder()
                .messageId(messageId)
                .roomId(messageDto.getRoomId())
                .senderNickname(nickname)
                .senderHash(senderHash)
                .senderChatRoomMemberId(senderChatRoomMember.getId())
                .content(messageDto.getContent())
                .imageCount(messageDto.getImageCount())
                .messageType(messageType)
                .extraData(messageDto.getExtraData())
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
                .messageType(messageType)
                .extraData(messageDto.getExtraData())
                .createDate(now)
                .modifiedDate(now)
                .build();

        chatBatchService.addMessageToQueue(chatMessage);

        sendChatNotification(chatRoom, sender, nickname, messageDto.getContent());

        if (isBotQuestion) {
            processBotQuestionAsync(chatRoom, sender, memberId, messageDto.getContent());
        }
    }

    private void processBotQuestionAsync(ChatRoom chatRoom, Member sender, Long memberId, String rawContent) {
        String cleanQuestion = extractCleanQuestion(rawContent);
        inuChatAiService.requestChat(memberId, cleanQuestion, List.of())
                .subscribe(aiAnswer -> {
                    try {
                        sendBotAnswer(chatRoom.getId(), sender, aiAnswer);
                    } catch (Exception e) {
                        log.error("챗불이 답변 브로드캐스트 실패: roomId={}, error={}", chatRoom.getId(), e.getMessage(), e);
                    }
                });
    }

    @Transactional
    public void sendBotAnswer(Long roomId, Member triggerUser, String aiAnswer) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElse(null);
        if (chatRoom == null) return;

        LocalDateTime now = LocalDateTime.now();
        Long messageId = TSID.fast().toLong();

        Set<String> activeUserIds = chatRedisService.getRoomUserIds(roomId);
        int totalJoined = chatRoomMemberRepository.countByChatRoomAndStatus(chatRoom, ChatMemberStatus.JOINED);
        int initialUnreadCount = Math.max(0, totalJoined - activeUserIds.size());

        String answerContent = aiAnswer != null ? aiAnswer : "";
        if (answerContent.length() > 4000) {
            answerContent = answerContent.substring(0, 3995) + "...";
        }

        ChatMessageResponseDto botResponseDto = ChatMessageResponseDto.builder()
                .messageId(messageId)
                .roomId(roomId)
                .senderNickname("챗불이")
                .senderHash("BOT_CHATBULI")
                .senderChatRoomMemberId(null)
                .content(answerContent)
                .imageCount(0)
                .messageType(MessageType.BOT_ANSWER)
                .unreadCount(initialUnreadCount)
                .createDate(now)
                .build();

        broadcastAndCache(roomId, botResponseDto);

        List<Long> activeMemberIds = activeUserIds.stream().map(Long::parseLong).toList();
        chatRoomMemberRepository.updateLastReadMessageIdByRoomAndMemberIds(chatRoom, activeMemberIds, messageId);

        ChatMessage botChatMessage = ChatMessage.builder()
                .id(messageId)
                .chatRoom(chatRoom)
                .sender(triggerUser)
                .content(answerContent)
                .senderNickname("챗불이")
                .imageCount(0)
                .messageType(MessageType.BOT_ANSWER)
                .createDate(now)
                .modifiedDate(now)
                .build();

        chatBatchService.addMessageToQueue(botChatMessage);

        String pushPreview = aiAnswer != null && aiAnswer.length() > 60 ? aiAnswer.substring(0, 60) + "..." : aiAnswer;
        sendChatNotification(chatRoom, triggerUser, "챗불이", pushPreview != null && !pushPreview.isBlank() ? pushPreview : "챗불이 답변이 도착했습니다.");
    }

    private String extractCleanQuestion(String rawContent) {
        if (rawContent == null) return "";
        String clean = rawContent.trim();
        if (clean.startsWith("[챗불이에게 질문]")) {
            clean = clean.substring("[챗불이에게 질문]".length()).trim();
        }
        if (clean.endsWith("[CHATBULI_QUESTION]")) {
            clean = clean.substring(0, clean.length() - "[CHATBULI_QUESTION]".length()).trim();
        }
        return clean;
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

        ChatRoomMember senderChatRoomMember = chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, sender)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_CHATROOM_MEMBER));

        String senderHash = getSenderHash(memberId);
        ChatMessageResponseDto responseDto = ChatMessageResponseDto.builder()
                .messageId(messageId)
                .roomId(chatRoom.getId())
                .senderNickname(nickname)
                .senderHash(senderHash)
                .senderChatRoomMemberId(senderChatRoomMember.getId())
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
                    boolean pushEnabled = m.getPushEnabled() != null ? m.getPushEnabled() : true;

                    String title = room.getTitle();
                    String senderName = "";
                    Long senderProfileImageNumber = null;
                    LocalDateTime lastMessageTime = room.getCreateDate();
                    String lastMessageContent = "아직 대화가 없습니다.";
                    String friendAlias = null;

                    // 개인 채팅방 처리
                    if (room.getType() == ChatRoomType.PERSONAL) {
                        if (room.isOfficial() && !member.getRoles().contains("ROLE_ADMIN")) {
                            title = "INTIP 운영자";
                        } else {
                            List<ChatRoomMember> roomMembers = chatRoomMemberRepository.findAllByChatRoomAndStatus(room,
                                    ChatMemberStatus.JOINED);
                            // 1:1 채팅방(참여 인원 2명)인 경우 항상 상대방 정보 사용
                            if (roomMembers.size() == 2) {
                                Optional<ChatRoomMember> otherMemberOpt = roomMembers.stream()
                                        .filter(orm -> !orm.getMember().getId().equals(memberId)).findFirst();
                                if (otherMemberOpt.isPresent()) {
                                    Member otherMember = otherMemberOpt.get().getMember();
                                    title = otherMember.getNickname();
                                    friendAlias = getFriendAlias(memberId, otherMember);
                                    senderProfileImageNumber = otherMember.getFireId(); // 상대방 프로필 번호로 고정
                                } else {
                                    title = "알 수 없음";
                                }
                            }
                            // 3인 이상 단체 개인방인 경우 프로필 번호를 null로 설정
                            else if (roomMembers.size() >= 3) {
                                senderProfileImageNumber = null;
                            }
                            
                            // 단체방인데 제목이 없는 경우만 상대방 닉네임들 노출
                            if ((title == null || title.isEmpty()) && roomMembers.size() > 2) {
                                title = roomMembers.stream()
                                        .filter(orm -> !orm.getMember().getId().equals(memberId))
                                        .map(crm -> crm.getMember().getNickname())
                                        .collect(Collectors.joining(", "));
                            } else if ((title == null || title.isEmpty())) {
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
                        // 오픈 채팅인 경우에만 마지막 채팅자의 프로필 이미지 번호 사용 (개인 채팅은 위에서 이미 결정됨)
                        if (room.getType() != ChatRoomType.PERSONAL) {
                            senderProfileImageNumber = lastMsg.getSender().getFireId();
                        }
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
                            .thumbnailUrl(room.getThumbnailUrl())
                            .friendAlias(friendAlias)
                            .pushEnabled(pushEnabled)
                            .build();
                })
                .sorted(Comparator.comparing(MyChatRoomResponseDto::getLastMessageTime, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatRoomResponseDto getOrCreatePersonalChatRoom(PersonalChatRoomRequestDto requestDto, Long memberId) {
        Member requester = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        List<Long> targetFriendIds = requestDto.getTargetFriendIds();
        if (targetFriendIds == null || targetFriendIds.isEmpty()) {
            throw new MyException(MyErrorCode.EMPTY_REQUEST);
        }

        List<Long> targetIds = targetFriendIds.stream()
                .map(friendId -> {
                    Friend friend = friendRepository.findById(friendId)
                            .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_FRIEND_REQUEST));
                    if (!friend.getRequester().getId().equals(memberId) && !friend.getReceiver().getId().equals(memberId)) {
                        throw new MyException(MyErrorCode.HAS_NOT_FRIEND_AUTHORIZATION);
                    }
                    if (friend.getStatus() != FriendStatus.ACCEPTED) {
                        throw new MyException(MyErrorCode.NOT_FRIEND);
                    }
                    return friend.getRequester().getId().equals(memberId) ? friend.getReceiver().getId() : friend.getRequester().getId();
                }).collect(Collectors.toList());

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
                        boolean pushEnabled = tm.getPushEnabled() != null ? tm.getPushEnabled() : true;
                        return ChatRoomResponseDto.of(room, currentParticipants.intValue(), getSenderHash(memberId),
                                isOwner, pushEnabled);
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
                        boolean pushEnabled = m.getPushEnabled() != null ? m.getPushEnabled() : true;
                        return ChatRoomResponseDto.of(room, memberCount, getSenderHash(memberId),
                                isOwner, pushEnabled);
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

        return ChatRoomResponseDto.of(chatRoom, allMemberIds.size(), getSenderHash(memberId), true, true);
    }

    @Transactional
    public ChatRoomResponseDto createOrGetPersonalChatRoomFromParticipant(Long roomId, Long chatRoomMemberId, Long memberId) {
        ChatRoom sourceRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));

        Member requester = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        ChatRoomMember requesterInSource = chatRoomMemberRepository.findByChatRoomAndMember(sourceRoom, requester)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_CHATROOM_MEMBER));
        if (requesterInSource.getStatus() != ChatMemberStatus.JOINED) {
            throw new MyException(MyErrorCode.NOT_CHATROOM_MEMBER);
        }

        ChatRoomMember targetInSource = chatRoomMemberRepository.findById(chatRoomMemberId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_CHATROOM_MEMBER));
        if (!targetInSource.getChatRoom().getId().equals(roomId)) {
            throw new MyException(MyErrorCode.NOT_CHATROOM_MEMBER);
        }
        if (targetInSource.getStatus() != ChatMemberStatus.JOINED) {
            throw new MyException(MyErrorCode.NOT_CHATROOM_MEMBER);
        }

        Member target = targetInSource.getMember();
        if (target.getId().equals(memberId)) {
            throw new MyException(MyErrorCode.NOT_SELF_CHAT);
        }

        if (blockRepository.existsByBlockerAndBlocked(requester, target) ||
                blockRepository.existsByBlockerAndBlocked(target, requester)) {
            throw new MyException(MyErrorCode.USER_NOT_FOUND);
        }

        boolean isAnonymous = sourceRoom.isAnonymous();

        // 1. 기존에 개설된 동일 조건의 1대1 방이 있는지 검색
        ChatRoom existingRoom = null;
        List<ChatRoomMember> myJoinedRooms = chatRoomMemberRepository.findAllByMemberAndStatus(requester, ChatMemberStatus.JOINED);
        for (ChatRoomMember m : myJoinedRooms) {
            ChatRoom room = m.getChatRoom();
            if (room.getType() == ChatRoomType.PERSONAL && !room.isOfficial()) {
                if (room.isAnonymous() == isAnonymous) {
                    if (!isAnonymous || (room.getSourceRoom() != null && room.getSourceRoom().getId().equals(roomId))) {
                        List<ChatRoomMember> roomMembers = chatRoomMemberRepository.findAllByChatRoomAndStatus(room, ChatMemberStatus.JOINED);
                        Set<Long> roomMemberIds = roomMembers.stream().map(cm -> cm.getMember().getId()).collect(Collectors.toSet());
                        Set<Long> bothIds = Set.of(memberId, target.getId());
                        if (roomMemberIds.equals(bothIds)) {
                            existingRoom = room;
                            break;
                        }
                    }
                }
            }
        }

        if (existingRoom != null) {
            boolean isOwner = existingRoom.getCreator().getId().equals(memberId);
            ChatRoomMember myMemberInNew = chatRoomMemberRepository.findByChatRoomAndMember(existingRoom, requester).orElseThrow();
            boolean pushEnabled = myMemberInNew.getPushEnabled() != null ? myMemberInNew.getPushEnabled() : true;
            return ChatRoomResponseDto.of(existingRoom, 2, getSenderHash(memberId), isOwner, pushEnabled);
        }

        // 2. 신규 채팅방 생성
        ChatRoom newRoom = ChatRoom.builder()
                .title("")
                .maxCapacity(100)
                .isAnonymous(isAnonymous)
                .type(ChatRoomType.PERSONAL)
                .creator(requester)
                .isOfficial(false)
                .sourceRoom(isAnonymous ? sourceRoom : null)
                .build();
        chatRoomRepository.save(newRoom);

        // 3. 참여 멤버들 등록
        ChatRoomMember newRequesterMember = ChatRoomMember.builder()
                .chatRoom(newRoom)
                .member(requester)
                .build();
        chatRoomMemberRepository.save(newRequesterMember);

        ChatRoomMember newTargetMember = ChatRoomMember.builder()
                .chatRoom(newRoom)
                .member(target)
                .build();
        chatRoomMemberRepository.save(newTargetMember);

        // 4. 익명 방일 경우, 출발지 단체방/오픈방에서 사용하던 닉네임 연동/복사
        if (isAnonymous) {
            chatRedisService.copyAnonymousNickname(roomId, newRoom.getId(), requester.getId());
            chatRedisService.copyAnonymousNickname(roomId, newRoom.getId(), target.getId());

            chatRedisService.getOrAssignAnonymousNickname(newRoom.getId(), requester.getId());
            chatRedisService.getOrAssignAnonymousNickname(newRoom.getId(), target.getId());
        }

        return ChatRoomResponseDto.of(newRoom, 2, getSenderHash(memberId), true, true);
    }

    @Transactional
    public ChatRoomResponseDto createChatRoom(ChatRoomCreateRequestDto requestDto, MultipartFile thumbnail, Long memberId) {
        Member creator = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        boolean isAnonymous = requestDto.getType() == ChatRoomType.PERSONAL ? false : requestDto.getIsAnonymous();

        ChatRoom chatRoom = ChatRoom.builder()
                .title(requestDto.getTitle())
                .description(requestDto.getDescription())
                .maxCapacity(requestDto.getMaxCapacity())
                .isAnonymous(isAnonymous)
                .type(requestDto.getType())
                .creator(creator)
                .isOfficial(false)
                .thumbnailUrl(requestDto.getThumbnailUrl())
                .build();
        chatRoomRepository.save(chatRoom);

        if (thumbnail != null && !thumbnail.isEmpty()) {
            try {
                String thumbnailUrl = imageService.saveChatRoomThumbnail(chatRoom.getId(), thumbnail, imagePath);
                chatRoom.updateInfo(chatRoom.getTitle(), chatRoom.getDescription(), thumbnailUrl, chatRoom.getMaxCapacity());
            } catch (IOException e) {
                log.error("채팅방 생성 썸네일 저장 중 오류 발생: ", e);
                throw new MyException(MyErrorCode.INVALID_INPUT);
            }
        }

        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .member(creator)
                .build();
        chatRoomMemberRepository.save(chatRoomMember);

        return ChatRoomResponseDto.of(chatRoom, 1, getSenderHash(memberId), true, true);
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
            if (m.getStatus() == ChatMemberStatus.KICKED) {
                throw new MyException(MyErrorCode.HAS_NOT_POST_AUTHORIZATION); // 강퇴당한 경우 재입장 불가
            }
            if (m.getStatus() == ChatMemberStatus.LEFT) {
                m.rejoin();
            }
            int memberCount = chatRoomMemberRepository.countByChatRoomAndStatus(chatRoom, ChatMemberStatus.JOINED);
            boolean isOwner = chatRoom.getCreator().getId().equals(memberId);
            boolean pushEnabled = m.getPushEnabled() != null ? m.getPushEnabled() : true;
            return ChatRoomResponseDto.of(chatRoom, memberCount, getSenderHash(memberId), isOwner, pushEnabled);
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
        boolean pushEnabled = chatRoomMember.getPushEnabled() != null ? chatRoomMember.getPushEnabled() : true;
        return ChatRoomResponseDto.of(chatRoom, finalParticipants, getSenderHash(memberId), isOwner, pushEnabled);
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

            String friendAlias = null;
            if (!chatRoom.isAnonymous()) {
                friendAlias = getFriendAlias(memberId, member);
            }

            return ChatRoomMemberResponseDto.builder()
                    .nickname(nickname)
                    .chatRoomMemberId(m.getId())
                    .studentId(chatRoom.isAnonymous() ? null : member.getMaskedStudentId())
                    .fireId(chatRoom.isAnonymous() ? null : member.getFireId())
                    .isMe(member.getId().equals(memberId))
                    .isOwner(member.getId().equals(chatRoom.getCreator().getId()))
                    .friendAlias(friendAlias)
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

        // 친구 별명 매핑을 위한 친구 목록 조회 (익명 방이 아닐 때만)
        Map<Long, String> friendAliasMap = new HashMap<>();
        if (!chatRoom.isAnonymous()) {
            List<kr.inuappcenterportal.inuportal.domain.member.model.Friend> friends = friendRepository.findAllByRequesterAndStatus(member, kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus.ACCEPTED);
            friends.forEach(f -> friendAliasMap.put(f.getReceiver().getId(), f.getRequesterAlias()));
            List<kr.inuappcenterportal.inuportal.domain.member.model.Friend> reverseFriends = friendRepository.findAllByReceiverAndStatus(member, kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus.ACCEPTED);
            reverseFriends.forEach(f -> friendAliasMap.put(f.getRequester().getId(), f.getReceiverAlias()));
        }

        int memberCount = chatRoomMemberRepository.countByChatRoomAndStatus(chatRoom, ChatMemberStatus.JOINED);
        boolean isOwner = chatRoom.getCreator().getId().equals(memberId);

        List<ChatRoomMember> roomMembersForMapping = chatRoomMemberRepository.findAllByChatRoom(chatRoom);
        Map<Long, Long> chatRoomMemberIdToMemberIdMap = roomMembersForMapping.stream()
                .collect(Collectors.toMap(ChatRoomMember::getId, rm -> rm.getMember().getId(), (e1, e2) -> e1));

        String title = chatRoom.getTitle();
        String friendAlias = null;
        Long otherMemberId = null;
        if (chatRoom.getType() == ChatRoomType.PERSONAL) {
            if (chatRoom.isOfficial() && !member.getRoles().contains("ROLE_ADMIN")) {
                title = "INTIP 운영자";
            } else {
                List<ChatRoomMember> roomParticipants = chatRoomMemberRepository.findAllByChatRoomAndStatus(chatRoom,
                        ChatMemberStatus.JOINED);
                if (roomParticipants.size() == 2) {
                    Optional<ChatRoomMember> otherMemberOpt = roomParticipants.stream()
                            .filter(m -> !m.getMember().getId().equals(memberId))
                            .findFirst();
                    if (otherMemberOpt.isPresent()) {
                        Member otherMember = otherMemberOpt.get().getMember();
                        otherMemberId = otherMember.getId();
                        title = otherMember.getNickname();
                        if (!chatRoom.isAnonymous()) {
                            friendAlias = getFriendAlias(memberId, otherMember);
                        }
                    } else {
                        title = "알 수 없음";
                    }
                }
            }
        }

        final String finalFriendAlias = friendAlias;
        final Long finalOtherMemberId = otherMemberId;
        final String myHash = getSenderHash(memberId);

        messages = messages.stream()
                .filter(msg -> msg.getSenderHash() == null || !blockedHashes.contains(msg.getSenderHash()))
                .map(msg -> {
                    int unread = (int) readIds.stream().filter(lastRead -> lastRead < msg.getMessageId()).count();
                    
                    // 별명 매핑 로직
                    String senderAlias = null;
                    if (!chatRoom.isAnonymous()) {
                        // 1. senderChatRoomMemberId가 있는 경우 (신규 메시지 또는 DB 메시지)
                        if (msg.getSenderChatRoomMemberId() != null) {
                            Long actualSenderMemberId = chatRoomMemberIdToMemberIdMap.get(msg.getSenderChatRoomMemberId());
                            if (actualSenderMemberId != null) {
                                senderAlias = friendAliasMap.get(actualSenderMemberId);
                            }
                        }
                        // 2. 1:1 채팅이고 senderChatRoomMemberId가 없으나(캐시된 구형 메시지) 본인이 아닌 경우
                        if (senderAlias == null && finalOtherMemberId != null && !myHash.equals(msg.getSenderHash())) {
                            senderAlias = finalFriendAlias;
                        }
                    }

                    return ChatMessageResponseDto.builder()
                            .messageId(msg.getMessageId())
                            .roomId(msg.getRoomId())
                            .senderNickname(msg.getSenderNickname())
                            .senderHash(msg.getSenderHash())
                            .senderChatRoomMemberId(msg.getSenderChatRoomMemberId())
                            .content(msg.getContent())
                            .imageCount(msg.getImageCount())
                            .unreadCount(unread)
                            .senderAlias(senderAlias)
                            .messageType(msg.getMessageType())
                            .extraData(msg.getExtraData())
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

        return ChatRoomResponseDto.builder()
                .id(chatRoom.getId())
                .title(title)
                .maxCapacity(chatRoom.getMaxCapacity())
                .isAnonymous(chatRoom.isAnonymous())
                .type(chatRoom.getType())
                .status(chatRoom.getStatus())
                .currentParticipants(memberCount)
                .createDate(chatRoom.getCreateDate())
                .myHash(myHash)
                .isOwner(isOwner)
                .isOfficial(chatRoom.isOfficial())
                .pushEnabled(chatRoomMember.getPushEnabled() != null ? chatRoomMember.getPushEnabled() : true)
                .friendAlias(friendAlias)
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

        // 친구 별명 매핑을 위한 친구 목록 조회 (익명 방이 아닐 때만)
        Map<Long, String> friendAliasMap = new HashMap<>();
        if (!chatRoom.isAnonymous()) {
            List<kr.inuappcenterportal.inuportal.domain.member.model.Friend> friends = friendRepository.findAllByRequesterAndStatus(member, kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus.ACCEPTED);
            friends.forEach(f -> friendAliasMap.put(f.getReceiver().getId(), f.getRequesterAlias()));
            List<kr.inuappcenterportal.inuportal.domain.member.model.Friend> reverseFriends = friendRepository.findAllByReceiverAndStatus(member, kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus.ACCEPTED);
            reverseFriends.forEach(f -> friendAliasMap.put(f.getRequester().getId(), f.getReceiverAlias()));
        }

        List<ChatRoomMember> roomMembersForMapping = chatRoomMemberRepository.findAllByChatRoom(chatRoom);
        Map<Long, Long> chatRoomMemberIdToMemberIdMap = roomMembersForMapping.stream()
                .collect(Collectors.toMap(ChatRoomMember::getId, rm -> rm.getMember().getId(), (e1, e2) -> e1));

        return olderMessages.stream()
                .map(this::convertToDto)
                .filter(dto -> dto.getSenderHash() == null || !blockedHashes.contains(dto.getSenderHash()))
                .map(dto -> {
                    int unread = (int) readIds.stream().filter(lastRead -> lastRead < dto.getMessageId()).count();
                    String senderAlias = null;
                    if (dto.getSenderChatRoomMemberId() != null) {
                        Long actualSenderMemberId = chatRoomMemberIdToMemberIdMap.get(dto.getSenderChatRoomMemberId());
                        if (actualSenderMemberId != null) {
                            senderAlias = friendAliasMap.get(actualSenderMemberId);
                        }
                    }
                    return ChatMessageResponseDto.builder()
                            .messageId(dto.getMessageId())
                            .roomId(dto.getRoomId())
                            .senderNickname(dto.getSenderNickname())
                            .senderHash(dto.getSenderHash())
                            .senderChatRoomMemberId(dto.getSenderChatRoomMemberId())
                            .content(dto.getContent())
                            .imageCount(dto.getImageCount())
                            .unreadCount(unread)
                            .senderAlias(senderAlias)
                            .messageType(dto.getMessageType())
                            .extraData(dto.getExtraData())
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

    @Transactional(readOnly = true)
    public Page<OpenChatRoomResponseDto> getOpenChatRooms(Long memberId, String search, Pageable pageable) {
        Page<ChatRoom> chatRooms = chatRoomRepository.findOpenChatRooms(search, pageable);

        Set<Long> joinedRoomIds = new HashSet<>();
        if (memberId != null) {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
            joinedRoomIds = chatRoomMemberRepository.findAllByMemberAndStatus(member, ChatMemberStatus.JOINED)
                    .stream().map(cm -> cm.getChatRoom().getId()).collect(Collectors.toSet());
        }

        final Set<Long> finalJoinedRoomIds = joinedRoomIds;
        return chatRooms.map(room -> {
            int participantCount = chatRoomMemberRepository.countByChatRoomAndStatus(room, ChatMemberStatus.JOINED);
            String ownerNickname = getDisplayNickname(room, room.getCreator());
            boolean isJoined = finalJoinedRoomIds.contains(room.getId());
            return OpenChatRoomResponseDto.of(room, participantCount, ownerNickname, isJoined);
        });
    }

    private String getDisplayNickname(ChatRoom chatRoom, Member member) {
        if (chatRoom.isOfficial() && member.getRoles().contains("ROLE_ADMIN")) {
            return chatRedisService.getOrAssignAdminNickname(chatRoom.getId(), member.getId());
        } else if (chatRoom.isAnonymous()) {
            return chatRedisService.getOrAssignAnonymousNickname(chatRoom.getId(), member.getId());
        } else {
            return member.getNickname();
        }
    }

    @Transactional
    public void updateRoomInfo(Long roomId, ChatRoomUpdateRequestDto requestDto, MultipartFile thumbnail, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        if (!chatRoom.getCreator().getId().equals(memberId) && !member.getRoles().contains("ROLE_ADMIN")) {
            throw new MyException(MyErrorCode.NOT_CHATROOM_OWNER);
        }

        int currentParticipants = chatRoomMemberRepository.countByChatRoomAndStatus(chatRoom, ChatMemberStatus.JOINED);
        if (requestDto.getMaxCapacity() < currentParticipants) {
            throw new MyException(MyErrorCode.INVALID_INPUT); // 현재 인원보다 적게 설정 불가
        }

        String thumbnailUrl = requestDto.getThumbnailUrl();
        if (thumbnail != null && !thumbnail.isEmpty()) {
            try {
                thumbnailUrl = imageService.saveChatRoomThumbnail(roomId, thumbnail, imagePath);
            } catch (IOException e) {
                log.error("채팅방 썸네일 저장 중 오류 발생: ", e);
                throw new MyException(MyErrorCode.INVALID_INPUT);
            }
        }

        chatRoom.updateInfo(requestDto.getTitle(), requestDto.getDescription(), thumbnailUrl,
                requestDto.getMaxCapacity());
    }

    @Transactional
    public void delegateOwner(Long roomId, ChatRoomDelegateRequestDto requestDto, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));
        if (!chatRoom.getCreator().getId().equals(memberId)) {
            throw new MyException(MyErrorCode.NOT_CHATROOM_OWNER);
        }

        ChatRoomMember newOwnerMember = chatRoomMemberRepository.findById(requestDto.getNewOwnerChatRoomMemberId())
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_CHATROOM_MEMBER));

        if (!newOwnerMember.getChatRoom().getId().equals(roomId)) {
            throw new MyException(MyErrorCode.NOT_CHATROOM_MEMBER);
        }

        if (newOwnerMember.getStatus() != ChatMemberStatus.JOINED) {
            throw new MyException(MyErrorCode.NOT_CHATROOM_MEMBER);
        }

        chatRoom.updateCreator(newOwnerMember.getMember());
    }

    @Transactional
    public void kickMember(Long roomId, Long targetChatRoomMemberId, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));
        if (!chatRoom.getCreator().getId().equals(memberId)) {
            throw new MyException(MyErrorCode.NOT_CHATROOM_OWNER);
        }

        ChatRoomMember chatRoomMember = chatRoomMemberRepository.findById(targetChatRoomMemberId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_CHATROOM_MEMBER));

        if (!chatRoomMember.getChatRoom().getId().equals(roomId)) {
            throw new MyException(MyErrorCode.NOT_CHATROOM_MEMBER);
        }

        if (chatRoomMember.getMember().getId().equals(memberId)) {
            throw new MyException(MyErrorCode.INVALID_INPUT); // 자신을 강퇴할 수 없음
        }

        chatRoomMember.kick();
        chatRedisService.removeUserFromRoom(roomId, chatRoomMember.getMember().getId());
        messagingTemplate.convertAndSend("/sub/room/" + roomId, "updated");
    }

    @Transactional(readOnly = true)
    public MemberProfileResponseDto getChatRoomMemberProfile(Long roomId, Long chatRoomMemberId, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));

        Member requester = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
        ChatRoomMember requesterMember = chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, requester)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_CHATROOM_MEMBER));
        if (requesterMember.getStatus() != ChatMemberStatus.JOINED) {
            throw new MyException(MyErrorCode.NOT_CHATROOM_MEMBER);
        }

        ChatRoomMember targetMember = chatRoomMemberRepository.findById(chatRoomMemberId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_CHATROOM_MEMBER));
        if (!targetMember.getChatRoom().getId().equals(roomId)) {
            throw new MyException(MyErrorCode.NOT_CHATROOM_MEMBER);
        }

        Member target = targetMember.getMember();

        if (blockRepository.existsByBlockerAndBlocked(target, requester) ||
            blockRepository.existsByBlockerAndBlocked(requester, target)) {
            throw new MyException(MyErrorCode.USER_NOT_FOUND);
        }

        String nickname;
        if (chatRoom.isOfficial() && target.getRoles().contains("ROLE_ADMIN")) {
            nickname = chatRedisService.getOrAssignAdminNickname(roomId, target.getId());
        } else if (chatRoom.isAnonymous()) {
            nickname = chatRedisService.getOrAssignAnonymousNickname(roomId, target.getId());
        } else {
            nickname = target.getNickname();
        }

        String studentId = null;
        Long fireId = null;
        String friendStatus = "NONE";
        Long friendId = null;
        String friendAlias = null;

        if (!chatRoom.isAnonymous()) {
            studentId = target.getMaskedStudentId();
            fireId = target.getFireId();

            Optional<Friend> friendOpt = friendRepository.findByRequesterAndReceiver(requester, target);
            if (friendOpt.isPresent()) {
                Friend f = friendOpt.get();
                friendStatus = f.getStatus() == FriendStatus.ACCEPTED ? "ACCEPTED" : "PENDING";
                friendId = f.getId();
                friendAlias = f.getRequesterAlias();
            } else {
                Optional<Friend> reverseFriendOpt = friendRepository.findByRequesterAndReceiver(target, requester);
                if (reverseFriendOpt.isPresent()) {
                    Friend f = reverseFriendOpt.get();
                    friendStatus = f.getStatus() == FriendStatus.ACCEPTED ? "ACCEPTED" : "RECEIVED";
                    friendId = f.getId();
                    friendAlias = f.getReceiverAlias();
                }
            }
        }

        return MemberProfileResponseDto.builder()
                .memberId(null)
                .nickname(nickname)
                .fireId(fireId)
                .department(chatRoom.isAnonymous() ? null : target.getDepartment())
                .maskedStudentId(studentId)
                .friendStatus(friendStatus)
                .friendAlias(friendAlias)
                .friendId(friendId)
                .build();
    }

    private void sendChatNotification(ChatRoom room, Member sender, String senderNickname, String content) {
        // 1. 현재 방에 접속 중인 사용자 ID 목록 가져오기
        Set<String> activeUserIds = chatRedisService.getRoomUserIds(room.getId());

        // 2. 알림 대상 멤버 전체 필터링 (참여 중인 멤버 중 발신자 제외, 현재 접속자 제외, 멤버 전역 알림 허용 여부 확인)
        List<ChatRoomMember> joinedMembers = chatRoomMemberRepository.findAllByChatRoomAndStatus(room,
                ChatMemberStatus.JOINED);

        List<ChatRoomMember> eligibleMembers = joinedMembers.stream()
                .filter(m -> m.getMember().getChatPushEnabled() != null ? m.getMember().getChatPushEnabled() : true)
                .filter(m -> !m.getMember().getId().equals(sender.getId())) // 발신자 제외
                .filter(m -> !activeUserIds.contains(String.valueOf(m.getMember().getId()))) // 현재 접속자 제외
                .toList();

        if (eligibleMembers.isEmpty()) {
            return;
        }


        // 3. 커스텀 닉네임(친구 별칭) 적용 및 무음 알림 여부에 따른 분할 그룹 구성
        Map<String, List<Long>> pushGroup = new HashMap<>();

        for (ChatRoomMember m : eligibleMembers) {
            String resolvedName = senderNickname;
            if (!room.isAnonymous() && !room.isOfficial()) {
                String alias = friendRepository.findByRequesterAndReceiver(m.getMember(), sender)
                        .filter(f -> f.getStatus() == FriendStatus.ACCEPTED)
                        .map(kr.inuappcenterportal.inuportal.domain.member.model.Friend::getRequesterAlias)
                        .orElseGet(() -> friendRepository.findByRequesterAndReceiver(sender, m.getMember())
                                .filter(f -> f.getStatus() == FriendStatus.ACCEPTED)
                                .map(kr.inuappcenterportal.inuportal.domain.member.model.Friend::getReceiverAlias)
                                .orElse(null));
                if (alias != null && !alias.trim().isEmpty()) {
                    resolvedName = alias;
                }
            }

            String title;
            String body;

            if (room.isOfficial()) {
                title = "INTIP 운영자";
                body = content;
            } else if (room.getType() == ChatRoomType.PERSONAL) {
                if (joinedMembers.size() == 2) {
                    title = resolvedName;
                    body = content;
                } else {
                    title = (room.getTitle() == null || room.getTitle().isEmpty()) ? "그룹 채팅" : room.getTitle();
                    body = resolvedName + ": " + content;
                }
            } else {
                title = room.getTitle();
                body = resolvedName + ": " + content;
            }

            boolean isMuted = m.getPushEnabled() != null && !m.getPushEnabled();
            String key = title + "|||" + body + "|||" + isMuted;
            pushGroup.computeIfAbsent(key, k -> new ArrayList<>()).add(m.getMember().getId());
        }

        // 4. 그룹별 비동기 푸시 발송
        pushGroup.forEach((key, memberIds) -> {
            String[] parts = key.split("\\|\\|\\|");
            if (parts.length >= 3) {
                String title = parts[0];
                String body = parts[1];
                boolean isMuted = Boolean.parseBoolean(parts[2]);
                fcmAsyncService.sendAsyncChatNotification(memberIds, title, body, room.getId(), isMuted);
            }
        });
    }

    private ChatMessageResponseDto convertToDto(ChatMessage message) {
        String nickname = message.getSenderNickname();
        if (message.getChatRoom().isOfficial() && message.getSender().getRoles().contains("ROLE_ADMIN")) {
            nickname = chatRedisService.getOrAssignAdminNickname(message.getChatRoom().getId(),
                    message.getSender().getId());
        }

        Long senderChatRoomMemberId = chatRoomMemberRepository.findByChatRoomAndMember(message.getChatRoom(), message.getSender())
                .map(ChatRoomMember::getId)
                .orElse(null);

        return ChatMessageResponseDto.builder()
                .messageId(message.getId())
                .roomId(message.getChatRoom().getId())
                .senderNickname(nickname)
                .senderHash(getSenderHash(message.getSender().getId()))
                .senderChatRoomMemberId(senderChatRoomMemberId)
                .content(message.getContent())
                .imageCount(message.getImageCount())
                .unreadCount(0)
                .messageType(message.getMessageType())
                .extraData(message.getExtraData())
                .createDate(message.getCreateDate())
                .build();
    }

    @Transactional
    public boolean toggleRoomPush(Long roomId, Long memberId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        ChatRoomMember chatRoomMember = chatRoomMemberRepository.findByChatRoomAndMember(room, member)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_CHATROOM_MEMBER));

        chatRoomMember.togglePush();
        return chatRoomMember.getPushEnabled();
    }

    @Transactional
    public ChatRoomResponseDto inviteFriends(Long roomId, ChatRoomInviteRequestDto requestDto, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));

        if (chatRoom.isAnonymous()) {
            throw new MyException(MyErrorCode.HAS_NOT_POST_AUTHORIZATION);
        }

        Member requester = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        ChatRoomMember requesterMember = chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, requester)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_CHATROOM_MEMBER));
        if (requesterMember.getStatus() != ChatMemberStatus.JOINED) {
            throw new MyException(MyErrorCode.NOT_CHATROOM_MEMBER);
        }

        int currentParticipants = chatRoomMemberRepository.countByChatRoomAndStatus(chatRoom, ChatMemberStatus.JOINED);
        if (currentParticipants < 3) {
            throw new MyException(MyErrorCode.INVALID_INPUT);
        }

        List<Long> targetFriendIds = requestDto.getTargetFriendIds();
        if (targetFriendIds == null || targetFriendIds.isEmpty()) {
            throw new MyException(MyErrorCode.EMPTY_REQUEST);
        }

        if (currentParticipants + targetFriendIds.size() > chatRoom.getMaxCapacity()) {
            throw new MyException(MyErrorCode.CHATROOM_FULL);
        }

        List<Member> targetMembers = new ArrayList<>();
        for (Long friendId : targetFriendIds) {
            Friend friend = friendRepository.findById(friendId)
                    .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_FRIEND_REQUEST));

            if (friend.getStatus() != FriendStatus.ACCEPTED) {
                throw new MyException(MyErrorCode.NOT_FRIEND);
            }

            if (!friend.getRequester().getId().equals(memberId) && !friend.getReceiver().getId().equals(memberId)) {
                throw new MyException(MyErrorCode.HAS_NOT_FRIEND_AUTHORIZATION);
            }

            Member target = friend.getRequester().getId().equals(memberId) ? friend.getReceiver() : friend.getRequester();

            if (blockRepository.existsByBlockerAndBlocked(requester, target) ||
                    blockRepository.existsByBlockerAndBlocked(target, requester)) {
                throw new MyException(MyErrorCode.USER_NOT_FOUND);
            }

            Optional<ChatRoomMember> existing = chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, target);
            if (existing.isPresent() && existing.get().getStatus() == ChatMemberStatus.JOINED) {
                continue;
            }

            targetMembers.add(target);
        }

        if (targetMembers.isEmpty()) {
            throw new MyException(MyErrorCode.INVALID_INPUT);
        }

        for (Member target : targetMembers) {
            Optional<ChatRoomMember> optMember = chatRoomMemberRepository.findByChatRoomAndMember(chatRoom, target);
            if (optMember.isPresent()) {
                optMember.get().rejoin();
            } else {
                ChatRoomMember newMember = ChatRoomMember.builder()
                        .chatRoom(chatRoom)
                        .member(target)
                        .build();
                chatRoomMemberRepository.save(newMember);
            }
        }

        // 입장 알림 시스템 메시지 생성 및 발송
        String invitedNicknames = targetMembers.stream()
                .map(Member::getNickname)
                .collect(Collectors.joining(", "));
        String systemContent = requester.getNickname() + "님이 " + invitedNicknames + "님을 초대했습니다.";

        Long messageId = TSID.fast().toLong();
        LocalDateTime now = LocalDateTime.now();

        ChatMessage chatMessage = ChatMessage.builder()
                .id(messageId)
                .chatRoom(chatRoom)
                .sender(requester)
                .content(systemContent)
                .senderNickname("알림")
                .imageCount(0)
                .createDate(now)
                .modifiedDate(now)
                .build();
        chatMessageRepository.save(chatMessage);

        ChatMessageResponseDto systemMessageDto = ChatMessageResponseDto.builder()
                .messageId(messageId)
                .roomId(chatRoom.getId())
                .senderNickname("알림")
                .senderHash(getSenderHash(memberId))
                .senderChatRoomMemberId(requesterMember.getId())
                .content(systemContent)
                .imageCount(0)
                .unreadCount(0)
                .createDate(now)
                .build();

        broadcastAndCache(chatRoom.getId(), systemMessageDto);

        // 푸시 알림 발송 (새로 초대된 멤버들에게 알림 발송)
        List<Long> targetMemberIds = targetMembers.stream().map(Member::getId).toList();
        fcmAsyncService.sendAsyncChatNotification(
                targetMemberIds,
                chatRoom.getTitle() != null && !chatRoom.getTitle().isEmpty() ? chatRoom.getTitle() : "그룹 채팅",
                requester.getNickname() + "님이 회원님을 그룹 채팅에 초대했습니다.",
                chatRoom.getId(),
                false
        );

        int finalParticipants = chatRoomMemberRepository.countByChatRoomAndStatus(chatRoom, ChatMemberStatus.JOINED);
        boolean isOwner = chatRoom.getCreator().getId().equals(memberId);
        boolean pushEnabled = requesterMember.getPushEnabled() != null ? requesterMember.getPushEnabled() : true;
        return ChatRoomResponseDto.of(chatRoom, finalParticipants, getSenderHash(memberId), isOwner, pushEnabled);
    }

    private String getFriendAlias(Long viewerId, Member target) {
        if (viewerId == null || target == null) return null;

        Optional<kr.inuappcenterportal.inuportal.domain.member.model.Friend> friendOpt = friendRepository.findByRequesterAndReceiver(
                memberRepository.findById(viewerId).orElse(null), target);
        if (friendOpt.isPresent()) {
            kr.inuappcenterportal.inuportal.domain.member.model.Friend f = friendOpt.get();
            if (f.getStatus() == kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus.ACCEPTED) return f.getRequesterAlias();
        }

        Optional<kr.inuappcenterportal.inuportal.domain.member.model.Friend> reverseFriendOpt = friendRepository.findByRequesterAndReceiver(
                target, memberRepository.findById(viewerId).orElse(null));
        if (reverseFriendOpt.isPresent()) {
            kr.inuappcenterportal.inuportal.domain.member.model.Friend f = reverseFriendOpt.get();
            if (f.getStatus() == kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus.ACCEPTED) return f.getReceiverAlias();
        }

        return null;
    }
}
