package com.example.wegather.group.domain.entity;

import com.example.wegather.global.BaseTimeEntity;
import com.example.wegather.group.domain.vo.RecruitingType;
import com.example.wegather.global.vo.SmallGroupStatus;
import com.example.wegather.group.domain.vo.SmallGroupMemberType;
import com.example.wegather.interest.domain.Interest;
import com.example.wegather.member.domain.entity.Member;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicInsert
@Entity
@Table(name = "SMALL_GROUP")
public class SmallGroup extends BaseTimeEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(fetch = FetchType.LAZY)
  private Member leader;
  private String path;
  private String name;
  private String shortDescription;
  @Lob
  @Basic(fetch = FetchType.EAGER)
  private String fullDescription;
  private String image;
  private String banner;
  private Long maxMemberCount;
  private LocalDateTime recruitingUpdatedDateTime;
  private LocalDateTime publishedDateTime;
  private LocalDateTime closedDateTime;
  private boolean recruiting = false;
  @Enumerated(EnumType.STRING)
  private RecruitingType recruitingType;
  private boolean published = false;
  private boolean closed = false;
  private boolean useBanner = false;
  @OneToMany(mappedBy = "smallGroup")
  private Set<SmallGroupMember> members = new HashSet<>();
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "smallGroup", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<SmallGroupInterest> smallGroupInterests = new HashSet<>();

  @Builder
  public SmallGroup(String path, String name, String shortDescription, String fullDescription ,Member leader,
      Long maxMemberCount) {
    this.path = path;
    this.name = name;
    this.shortDescription = shortDescription;
    this.fullDescription = fullDescription;
    this.leader = leader;
    this.maxMemberCount = maxMemberCount;
  }

  public void updateSmallGroupDescription(String shortDescription, String fullDescription) {
    this.shortDescription = shortDescription;
    this.fullDescription = fullDescription;
  }

  public boolean isLeader(Long id) {
    return Objects.equals(leader.getId(), id);
  }

  public void addInterest(Interest interest) {
    this.smallGroupInterests.add(SmallGroupInterest.of(this, interest));
  }

  public void removeInterest(Interest interest) {
    this.smallGroupInterests.remove(SmallGroupInterest.of(this, interest));
  }

  public List<String> getInterests() {
    return smallGroupInterests.stream()
        .map(smallGroupInterest -> smallGroupInterest.getInterest().getName())
        .collect(Collectors.toList());
  }

  public boolean isExceedMaxMember(Long nowCount) {
    return maxMemberCount <= nowCount;
  }

  /**
   * 회원 ID가 해당 소모임의 회원에 포합되어 있는지 반환합니다.
   * @param memberId 회원 ID
   * @return
   */
  public boolean isMemberOrManager(Long memberId) {
    return containsInMember(memberId);
  }

  /**
   * 회원 ID가 해당 소모임의 관리자인지 판단합니다.
   * @param memberId 회원 ID
   * @return
   */
  public boolean isManager(Long memberId) {
    return members.stream()
        .filter(member -> member.getSmallGroupMemberType().equals(SmallGroupMemberType.MANAGER))
        .map(SmallGroupMember::getMemberId)
        .anyMatch(member -> member.equals(memberId));
  }

  /**
   * 소모임이 가입 가능한지 반환합니다.
   * @return
   */
  public boolean isJoinable() {
    return isPublished() && isRecruiting() && !isClosed();
  }

  public int getCurrentMemberCount() {
    return this.members.size();
  }

  public void updateBanner(String banner) {
    this.banner = banner;
  }

  /**
   * 배너 사용 여부를 변경합니다.
   * true <-> false
   */
  public void toggleUseBanner() {
    useBanner = !useBanner;
  }

  /**
   * 소모임의 이미지를 새 이미지로 업데이트 합니다.
   * @param newImage 새 이미지
   */
  public void updateImage(String newImage) {
    this.image = newImage;
  }

  private boolean containsInMember(Long memberId) {
    return members.stream().map(SmallGroupMember::getMemberId)
        .anyMatch(memberId::equals);
  }

  /**
   * 소모임을 공개 합니다.
   */
  public void publish() {
    this.published = true;
    this.publishedDateTime = LocalDateTime.now();
  }

  /**
   * 소모임 인원 모집을 시작합니다.
   * @param recruitingType 소모임 모집 방식
   */
  public void openRecruiting(RecruitingType recruitingType) {
    this.recruiting = true;
    this.recruitingType = recruitingType;
    this.recruitingUpdatedDateTime = LocalDateTime.now();
  }

  /**
   * 소모임을 종료합니다.
   */
  public void close() {
    this.closed = true;
    this.closedDateTime = LocalDateTime.now();
  }

  /**
   * 소모임 상태를 반환합니다.
   * @return
   */
  public SmallGroupStatus getStatus() {
    if (this.closed) {
      return SmallGroupStatus.CLOSED;
    }
    if (this.recruiting) {
      return SmallGroupStatus.RECRUITING;
    }
    if (this.published) {
      return SmallGroupStatus.PUBLISHED;
    }
    return SmallGroupStatus.BEFORE_OPEN;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SmallGroup that = (SmallGroup) o;
    return id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
