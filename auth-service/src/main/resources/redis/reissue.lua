local currentHash = redis.call('GET', KEYS[1])

if currentHash == false then
    return 0
end

if currentHash ~= ARGV[1] then
    return 0
end

local refreshTtl = tonumber(ARGV[3])

if refreshTtl == nil or refreshTtl <= 0 then
    return 0
end

redis.call(
    'SET',
    KEYS[1],
    ARGV[2],
    'PX',
    refreshTtl
)

redis.call(
    'PEXPIRE',
    KEYS[2],
    refreshTtl
)

return 1