local current = redis.call('GET', KEYS[1])
if not current then current = 0 end
local result = tonumber(current) + tonumber(ARGV[1])
redis.call('SET', KEYS[1], result)
return result