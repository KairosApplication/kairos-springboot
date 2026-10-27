if redis.call('EXISTS', KEYS[1]) == 0 then return 0 end
-- Empty proof is reserved for a session ID from a verified access JWT.
if ARGV[1] == '' or redis.call('HEXISTS', KEYS[1], 't:' .. ARGV[1]) == 1 then
    redis.call('HSET', KEYS[1], 'revoked', '1')
    return 1
end
return 0
