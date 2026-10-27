-- A three-second overlap returns the SAME successor to concurrent requests.
-- Only hashes/timestamps are stored. The API derives the successor using HMAC.
local expires = tonumber(redis.call('HGET', KEYS[1], 'expires'))
if not expires then return {} end
local now = redis.call('TIME')
local millis = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000)
if expires <= millis or redis.call('HGET', KEYS[1], 'revoked') ~= '0' then return {} end
local field = 't:' .. ARGV[1]
local state = redis.call('HGET', KEYS[1], field)
if not state then return {} end
if state == 'used' then
    local overlap = tonumber(redis.call('HGET', KEYS[1], 'overlap:' .. ARGV[1]))
    if not overlap or millis > overlap or redis.call('HGET', KEYS[1], 't:' .. ARGV[2]) ~= 'active' then
        redis.call('HSET', KEYS[1], 'revoked', '1')
        return {}
    end
elseif state == 'active' then
    if ARGV[3] == '1' then
        redis.call('HSET', KEYS[1], field, 'used', 't:' .. ARGV[2], 'active',
            'overlap:' .. ARGV[1], tostring(millis + 3000))
    end
else
    return {}
end
return {redis.call('HGET', KEYS[1], 'uid'), tostring(expires)}
