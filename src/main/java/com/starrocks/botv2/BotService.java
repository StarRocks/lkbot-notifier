package com.starrocks.botv2;

public interface BotService {
    void start(String userDescription);
    void success(String userDescription);
    void failed();
    void abort();
}
