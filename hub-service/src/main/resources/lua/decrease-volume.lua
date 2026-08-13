local current = redis.call('GET', KEYS[1])
if not current then return 0 end
local result = tonumber(current) - tonumber(ARGV[1])
if result < 0 then result = 0 end
redis.call('SET', KEYS[1], result)
return result