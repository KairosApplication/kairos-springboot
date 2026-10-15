-- All metadata and token hashes share a single hash and therefore a Cluster hash slot.
local expires = tonumber(redis.call('HGET', KEYS[1], 'expires'))
if not expires then return {} end
local now = redis.call('TIME')
local millis = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000)
if expires <= millis or redis.call('HGET', KEYS[1], 'revoked') ~= '0' then return {} end
local field = 't:' .. ARGV[1]
local state = redis.call('HGET', KEYS[1], field)
-- Unknown hashes must never revoke a family, even if the locator was guessed.
if not state then return {} end
if state == 'used' then
    redis.call('HSET', KEYS[1], 'revoked', '1')
    return {}
end
if state ~= 'active' then return {} end
if ARGV[2] ~= '' then
    redis.call('HSET', KEYS[1], field, 'used', 't:' .. ARGV[2], 'active')
end
return {redis.call('HGET', KEYS[1], 'uid'), tostring(expires)}
