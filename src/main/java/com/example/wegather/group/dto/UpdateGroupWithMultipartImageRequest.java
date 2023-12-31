package com.example.wegather.group.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class UpdateGroupWithMultipartImageRequest {
  private String shortDescription;
  private String fullDescription;
}
