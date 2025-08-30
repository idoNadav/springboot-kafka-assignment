package com.assignment.commonmodel.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCheckResultEvent {

    @NonNull
    private String orderId;

    @NonNull
    private InventoryStatus status;

    private List<InventoryIssue> issues;

}
