package kr.inuappcenterportal.inuportal.domain.book.enums;

import lombok.Getter;

@Getter
public enum TransactionStatus {

    AVAILABLE, COMPLETED, DELETED;

    public TransactionStatus toggle() {
        return this == COMPLETED ? AVAILABLE : COMPLETED;
    }

    public TransactionStatus delete() { return DELETED; }
}
