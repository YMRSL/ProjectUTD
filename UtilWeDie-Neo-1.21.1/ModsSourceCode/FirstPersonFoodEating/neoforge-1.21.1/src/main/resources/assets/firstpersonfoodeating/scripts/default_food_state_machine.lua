local track_line_top = {value = 0}
local static_track_top = {value = 0}

local function increment(obj)
    obj.value = obj.value + 1
    return obj.value - 1
end

local STATIC_TRACK_LINE = increment(track_line_top)
local BASE_TRACK = increment(static_track_top)
local MAIN_TRACK = increment(static_track_top)
local MOVE_TRACK = increment(static_track_top)

local main_track_states = {
    start = {},
    idle = {},
    inspect = {},
    use = {},
    final = {
        isfinal = false
    },
    INPUT_INSPECT_RETREAT = "inspect_retreat"
}

local movement_states = {
    idle = {},
    run = {},
    walk = {}
}
local USE_RUN_TRANSITION_CLIPS = true
local STATE_BLEND_TIME = 0.2

local function play_loop_track(context, track_index, clip, fallback, transition)
    local track = context:getTrack(STATIC_TRACK_LINE, track_index)
    local selected = clip
    if (not context:hasAnimation(selected)) then
        selected = fallback
    end
    if (selected ~= nil and context:hasAnimation(selected)) then
        context:runAnimation(selected, track, true, LOOP, transition)
    end
end

local function resolve_use_clip(context)
    local selected = nil
    if (context.getUseClip ~= nil) then
        selected = context:getUseClip()
    end
    if ((selected == nil or selected == "") and context.getUseClipName ~= nil) then
        selected = context:getUseClipName()
    end
    if (selected ~= nil and selected ~= "" and context:hasAnimation(selected)) then
        return selected
    end
    if (context:hasAnimation("use")) then
        return "use"
    end
    return nil
end

function main_track_states.start.transition(this, context, input)
    if (input == INPUT_DRAW) then
        context:runAnimation("draw", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, STATE_BLEND_TIME)
        return this.main_track_states.idle
    end
end

function main_track_states.idle.transition(this, context, input)
    if (input == INPUT_PUT_AWAY) then
        context:runAnimation("put_away", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_HOLD, context:getPutAwayTime())
        this.main_track_states.final.isfinal = true
        return this.main_track_states.final
    end
    if (input == INPUT_DRAW) then
        context:runAnimation("draw", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, STATE_BLEND_TIME)
        return this.main_track_states.idle
    end
    if (input == INPUT_INSPECT) then
        context:runAnimation("inspect", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, STATE_BLEND_TIME)
        return this.main_track_states.inspect
    end
    if (input == INPUT_USE or input == INPUT_RELOAD) then
        local use_clip = resolve_use_clip(context)
        if (use_clip ~= nil) then
            context:runAnimation(use_clip, context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, STATE_BLEND_TIME)
            return this.main_track_states.use
        end
    end
end

function main_track_states.inspect.update(this, context)
    if (context:isStopped(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))) then
        context:trigger(this.main_track_states.INPUT_INSPECT_RETREAT)
    end
end

function main_track_states.inspect.transition(this, context, input)
    if (input == this.main_track_states.INPUT_INSPECT_RETREAT) then
        return this.main_track_states.idle
    end
    if (input == INPUT_USE or input == INPUT_RELOAD) then
        context:stopAnimation(context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK))
        local use_clip = resolve_use_clip(context)
        if (use_clip ~= nil) then
            context:runAnimation(use_clip, context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_STOP, STATE_BLEND_TIME)
            return this.main_track_states.use
        end
    end
    if (input == INPUT_PUT_AWAY) then
        context:runAnimation("put_away", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_HOLD, context:getPutAwayTime())
        return this.main_track_states.final
    end
end

function main_track_states.use.update(this, context)
    if (not context:isUsingItem()) then
        context:trigger(INPUT_USE_END)
    end
end

function main_track_states.use.transition(this, context, input)
    if (input == INPUT_USE_END) then
        return this.main_track_states.idle
    end
    if (input == INPUT_PUT_AWAY) then
        context:runAnimation("put_away", context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), false, PLAY_ONCE_HOLD, context:getPutAwayTime())
        return this.main_track_states.final
    end
end

function movement_states.idle.update(this, context)
    local base_track = context:getTrack(STATIC_TRACK_LINE, BASE_TRACK)
    if (context:isStopped(base_track) or context:isHolding(base_track)) then
        play_loop_track(context, BASE_TRACK, "static_idle", nil, STATE_BLEND_TIME)
    end
end

function movement_states.idle.entry(this, context)
    play_loop_track(context, BASE_TRACK, "static_idle", nil, STATE_BLEND_TIME)
end

function movement_states.idle.transition(this, context, input)
    if (input == INPUT_RUN) then
        return this.movement_states.run
    end
    if (input == INPUT_WALK) then
        return this.movement_states.walk
    end
end

function movement_states.run.entry(this, context)
    local move_track = context:getTrack(STATIC_TRACK_LINE, MOVE_TRACK)
    play_loop_track(context, BASE_TRACK, "static_idle", nil, STATE_BLEND_TIME)
    if (USE_RUN_TRANSITION_CLIPS and context:hasAnimation("run_start")) then
        context:runAnimation("run_start", move_track, true, PLAY_ONCE_STOP, STATE_BLEND_TIME)
        return
    end
    play_loop_track(context, MOVE_TRACK, "run", nil, STATE_BLEND_TIME)
end

function movement_states.run.update(this, context)
    local base_track = context:getTrack(STATIC_TRACK_LINE, BASE_TRACK)
    local move_track = context:getTrack(STATIC_TRACK_LINE, MOVE_TRACK)
    if (context:isStopped(base_track) or context:isHolding(base_track)) then
        play_loop_track(context, BASE_TRACK, "static_idle", nil, STATE_BLEND_TIME)
    end
    if (context:isStopped(move_track) or context:isHolding(move_track)) then
        play_loop_track(context, MOVE_TRACK, "run", nil, STATE_BLEND_TIME)
    end
end

function movement_states.run.transition(this, context, input)
    if (input == INPUT_IDLE) then
        local move_track = context:getTrack(STATIC_TRACK_LINE, MOVE_TRACK)
        if (USE_RUN_TRANSITION_CLIPS and context:hasAnimation("run_end")) then
            context:runAnimation("run_end", move_track, true, PLAY_ONCE_STOP, STATE_BLEND_TIME)
        end
        return this.movement_states.idle
    end
    if (input == INPUT_WALK) then
        local move_track = context:getTrack(STATIC_TRACK_LINE, MOVE_TRACK)
        if (USE_RUN_TRANSITION_CLIPS and context:hasAnimation("run_end")) then
            context:runAnimation("run_end", move_track, true, PLAY_ONCE_STOP, STATE_BLEND_TIME)
        end
        return this.movement_states.walk
    end
end

function movement_states.walk.entry(this, context)
    play_loop_track(context, BASE_TRACK, "walk", "static_idle", STATE_BLEND_TIME)
end

function movement_states.walk.update(this, context)
    local track = context:getTrack(STATIC_TRACK_LINE, BASE_TRACK)
    if (context:isStopped(track) or context:isHolding(track)) then
        play_loop_track(context, BASE_TRACK, "walk", "static_idle", STATE_BLEND_TIME)
    end
end

function movement_states.walk.transition(this, context, input)
    if (input == INPUT_IDLE) then
        return this.movement_states.idle
    end
    if (input == INPUT_RUN) then
        return this.movement_states.run
    end
end

local M = {
    track_line_top = track_line_top,
    static_track_top = static_track_top,
    STATIC_TRACK_LINE = STATIC_TRACK_LINE,
    BASE_TRACK = BASE_TRACK,
    MAIN_TRACK = MAIN_TRACK,
    MOVE_TRACK = MOVE_TRACK,
    main_track_states = main_track_states,
    movement_states = movement_states
}

function M:initialize(context)
    context:ensureTrackLineSize(track_line_top.value)
    context:ensureTracksAmount(STATIC_TRACK_LINE, static_track_top.value)
end

function M:exit(context)
end

function M:states()
    return {
        self.main_track_states.start,
        self.movement_states.idle
    }
end

return M
