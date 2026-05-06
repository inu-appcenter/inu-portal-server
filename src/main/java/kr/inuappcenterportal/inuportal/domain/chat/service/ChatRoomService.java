package kr.inuappcenterportal.inuportal.domain.chat.service;

import kr.inuappcenterportal.inuportal.domain.chat.model.ChatRoom;
import kr.inuappcenterportal.inuportal.domain.chat.model.ChatRoomMember;
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
    }
}
