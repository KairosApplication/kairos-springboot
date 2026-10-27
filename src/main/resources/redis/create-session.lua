if redis.call('EXISTS', KEYS[1]) == 1 then return 0 end
local now = redis.call('TIME')
local expires = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000) + tonumber(ARGV[3])
redis.call('HSET', KEYS[1], 'uid', ARGV[1], 'expires', expires, 'revoked', '0', 't:' .. ARGV[2], 'active')
redis.call('PEXPIREAT', KEYS[1], expires)
return expires
