-- Resolves container ID from the Fluent Bit tag to a service_name
-- by reading Docker's container config file.
--
-- Tag format from tail input: docker.<container_id>-json.log
-- Docker config path: /var/lib/docker/containers/<id>/config.v2.json

local cache = {}

function set_service_name(tag, timestamp, record)
    -- Extract container ID from the tag
    local container_id = tag:match("^docker%.(.+)")
    if not container_id then
        record["service_name"] = "unknown"
        return 2, timestamp, record
    end

    -- Check cache first
    if cache[container_id] then
        record["service_name"] = cache[container_id]
        return 2, timestamp, record
    end

    -- Try to read container name from Docker config
    local config_path = "/var/lib/docker/containers/" .. container_id .. "/config.v2.json"
    local f = io.open(config_path, "r")
    if f then
        local content = f:read("*a")
        f:close()
        -- Extract Name field: "Name":"/zenandops-auth-service"
        local name = content:match('"Name":"/?([^"]+)"')
        if name then
            local service_name = resolve_service_name(name)
            cache[container_id] = service_name
            record["service_name"] = service_name
            return 2, timestamp, record
        end
    end

    record["service_name"] = "unknown"
    return 2, timestamp, record
end

function resolve_service_name(container_name)
    local map = {
        ["zenandops-auth-service"]      = "zenandops-auth",
        ["zenandops-dashboard-service"] = "zenandops-dashboard",
        ["zenandops-cmdb-service"]      = "zenandops-cmdb",
        ["zenandops-gateway-service"]   = "zenandops-gateway",
        ["zenandops-frontend"]          = "zenandops-frontend",
        ["zenandops-mongodb"]           = "zenandops-mongodb",
        ["zenandops-mongodb-exporter"]  = "zenandops-mongodb-exporter",
        ["zenandops-kafka"]             = "zenandops-kafka",
        ["zenandops-kafka-exporter"]    = "zenandops-kafka-exporter",
        ["zenandops-prometheus"]        = "zenandops-prometheus",
        ["zenandops-node-exporter"]     = "zenandops-node-exporter",
        ["zenandops-cadvisor"]          = "zenandops-cadvisor",
        ["zenandops-fluent-bit"]        = "zenandops-fluent-bit",
        ["zenandops-grafana"]           = "zenandops-grafana",
        ["zenandops-loki"]              = "zenandops-loki",
        ["zenandops-mimir"]             = "zenandops-mimir",
        ["zenandops-tempo"]             = "zenandops-tempo",
    }
    return map[container_name] or container_name
end
