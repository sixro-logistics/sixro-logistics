local ttl = tonumber(ARGV[2])

redis.call('DEL', KEYS[1])
redis.call('DEL', KEYS[2])

if ARGV[1] ~= ''
        and ttl ~= nil
        and ttl > 0 then

    redis.call(
        'SET',
        KEYS[3],
        'logout',
        'PX',
        ttl
    )
end

return 1