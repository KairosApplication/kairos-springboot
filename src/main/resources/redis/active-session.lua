local expires = tonumber(redis.call('HGET', KEYS[1], 'expires'))
if not expires then return 0 end
local now = redis.call('TIME')
local millis = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000)
if expires <= millis or redis.call('HGET', KEYS[1], 'revoked') ~= '0' then return 0 end
if redis.call('HGET', KEYS[1], 'uid') ~= ARGV[1] then return 0 end
return 1
