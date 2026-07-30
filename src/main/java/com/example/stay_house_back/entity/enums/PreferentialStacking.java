package com.example.stay_house_back.entity.enums;

/** 같은 group_id 안에서 우대 항목을 합치는 방식. */
public enum PreferentialStacking {

    /** 체크된 것 중 delta 최댓값 1개만 적용 (원문: "중복 적용 불가"). */
    EXCLUSIVE,

    /** 체크된 것 전부 합산 (원문: "중복 적용 가능"). */
    STACKABLE
}
