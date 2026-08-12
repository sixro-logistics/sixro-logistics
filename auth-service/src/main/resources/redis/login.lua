local ttl = tonumber(ARGV[3])

if ttl == nil or ttl <= 0 then
    return 0
end

redis.call(
    'SET',
    KEYS[1],
    ARGV[1],
    'PX',
    ttl
)

redis.call(
    'SET',
    KEYS[2],
    ARGV[2],
    'PX',
    ttl
)

return 1