package com.almang.inventory.global.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

public class InMemoryLogAppender extends AppenderBase<ILoggingEvent> {

    private static final int MAX_EVENTS = 1000;
    private static final Deque<ILoggingEvent> events = new ConcurrentLinkedDeque<>();

    @Override
    protected void append(ILoggingEvent eventObject) {
        events.addLast(eventObject);

        // 로그가 1000개가 넘으면 처음 로그부터 버림
        while (events.size() > MAX_EVENTS) {
            events.pollFirst();
        }
    }

    public static List<ILoggingEvent> getRecentEvents(int limit) {
        int size = events.size();
        int skip = Math.max(0, size - limit);

        return events.stream()
                .skip(skip)
                .collect(Collectors.toList());
    }
}
