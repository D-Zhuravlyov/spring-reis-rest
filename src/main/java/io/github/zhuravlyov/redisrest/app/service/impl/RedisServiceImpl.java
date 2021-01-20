package io.github.zhuravlyov.redisrest.app.service.impl;

import io.github.zhuravlyov.redisrest.app.dto.RedisMessageDto;
import io.github.zhuravlyov.redisrest.app.exception.InternalServerException;
import io.github.zhuravlyov.redisrest.app.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

@Log4j2
@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final String REDIS_HASH_PREFIX = "MSG";
    private final RedisTemplate<String, String> redisTemplate;
    private BoundZSetOperations<String, String> boundZSetOps;

    @PostConstruct
    public void initOpsForHash() {
        setOpsForHash(redisTemplate.boundZSetOps(REDIS_HASH_PREFIX));
    }

    @Override
    public void saveMessageToRedis(final RedisMessageDto redisMessageDto) {
        final double timestamp = (double) Instant.now().toEpochMilli();
        boundZSetOps.add(redisMessageDto.getContent(), timestamp);
        log.debug("Message successfully saved.");
    }

    @Override
    public Set<String> getLastMessage() {
        int offset = 0;
        int count = 1;
        final Set<String> messageResult = redisTemplate.opsForZSet().reverseRangeByScore(REDIS_HASH_PREFIX, Double.MIN_VALUE, Double.MAX_VALUE, offset, count);
        if(Objects.nonNull(messageResult) && messageResult.size() == 1){
            log.debug("Last message found.");
            return messageResult;
        }
        else {
            throw new InternalServerException("Result is 'null' or Multiple results found.");
        }
    }

    @Override
    public Set<String> getMessagesByTimeRange(final Instant start, final Instant end) {
        return boundZSetOps.rangeByScore(start.toEpochMilli(), end.toEpochMilli());
    }

    public void setOpsForHash(final BoundZSetOperations<String, String> boundZSetOps){
        this.boundZSetOps = boundZSetOps;
    }
}
