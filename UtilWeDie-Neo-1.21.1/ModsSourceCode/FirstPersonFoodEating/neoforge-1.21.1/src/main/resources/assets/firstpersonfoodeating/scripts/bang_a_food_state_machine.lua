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

local CLIP_DRAW = "i_bang_d.draw"
local CLIP_PUT_AWAY = "i_bang_d.put_away"
local CLIP_INSPECT = "i_bang_d.inspect"
local CLIP_USE = "i_bang_a.use"
local CLIP_STATIC_IDLE = "i_bang_d.static_idle"
local CLIP_RUN = "i_bang_d.run"
local CLIP_WALK = "i_bang_d.walk"
local CLIP_RUN_START = "i_bang_d.run_start"
local CLIP_RUN_END = "i_bang_d.run_end"

local function pick_clip(context, primary, fallback)
    if (primary ~= nil and context:hasAnimation(primary)) then
        return primary
    end
    if (fallback ~= nil and context:hasAnimation(fallback)) then
        return fallback
    end
    return nil
end

local function run_clip(context, track, primary, fallback, blending, play_type, transition)
    local selected = pick_clip(context, primary, fallback)
    if (selected ~= nil) then
        context:runAnimation(selected, track, blending, play_type, transition)
    end
end

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
    run_clip(context, track, clip, fallback, true, LOOP, transition)
end

function main_track_states.start.transition(this, context, input)
    if (input == INPUT_DRAW) then
        run_clip(context, context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), CLIP_DRAW, "draw", false, PLAY_ONCE_STOP, STATE_BLEND_TIME)
        return this.main_track_states.idle
    end
end

function main_track_states.idle.transition(this, context, input)
    if (input == INPUT_PUT_AWAY) then
        run_clip(context, context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), CLIP_PUT_AWAY, "put_away", false, PLAY_ONCE_HOLD, context:getPutAwayTime())
        this.main_track_states.final.isfinal = true
        return this.main_track_states.final
    end
    if (input == INPUT_DRAW) then
        run_clip(context, context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), CLIP_DRAW, "draw", false, PLAY_ONCE_STOP, STATE_BLEND_TIME)
        return this.main_track_states.idle
    end
    if (input == INPUT_INSPECT) then
        run_clip(context, context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), CLIP_INSPECT, "inspect", false, PLAY_ONCE_STOP, STATE_BLEND_TIME)
        return this.main_track_states.inspect
    end
    if (input == INPUT_USE or input == INPUT_RELOAD) then
        run_clip(context, context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), CLIP_USE, "use", false, PLAY_ONCE_STOP, STATE_BLEND_TIME)
        return this.main_track_states.use
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
        run_clip(context, context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), CLIP_USE, "use", false, PLAY_ONCE_STOP, STATE_BLEND_TIME)
        return this.main_track_states.use
    end
    if (input == INPUT_PUT_AWAY) then
        run_clip(context, context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), CLIP_PUT_AWAY, "put_away", false, PLAY_ONCE_HOLD, context:getPutAwayTime())
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
        run_clip(context, context:getTrack(STATIC_TRACK_LINE, MAIN_TRACK), CLIP_PUT_AWAY, "put_away", false, PLAY_ONCE_HOLD, context:getPutAwayTime())
        return this.main_track_states.final
    end
end

function movement_states.idle.update(this, context)
    local base_track = context:getTrack(STATIC_TRACK_LINE, BASE_TRACK)
    if (context:isStopped(base_track) or context:isHolding(base_track)) then
        play_loop_track(context, BASE_TRACK, CLIP_STATIC_IDLE, "static_idle", STATE_BLEND_TIME)
    end
end

function movement_states.idle.entry(this, context)
    play_loop_track(context, BASE_TRACK, CLIP_STATIC_IDLE, "static_idle", STATE_BLEND_TIME)
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
    play_loop_track(context, BASE_TRACK, CLIP_STATIC_IDLE, "static_idle", STATE_BLEND_TIME)
    if (USE_RUN_TRANSITION_CLIPS and context:hasAnimation(CLIP_RUN_START)) then
        context:runAnimation(CLIP_RUN_START, move_track, true, PLAY_ONCE_STOP, STATE_BLEND_TIME)
        return
    end
    play_loop_track(context, MOVE_TRACK, CLIP_RUN, nil, STATE_BLEND_TIME)
end

function movement_states.run.update(this, context)
    local base_track = context:getTrack(STATIC_TRACK_LINE, BASE_TRACK)
    local move_track = context:getTrack(STATIC_TRACK_LINE, MOVE_TRACK)
    if (context:isStopped(base_track) or context:isHolding(base_track)) then
        play_loop_track(context, BASE_TRACK, CLIP_STATIC_IDLE, "static_idle", STATE_BLEND_TIME)
    end
    if (context:isStopped(move_track) or context:isHolding(move_track)) then
        play_loop_track(context, MOVE_TRACK, CLIP_RUN, nil, STATE_BLEND_TIME)
    end
end

function movement_states.run.transition(this, context, input)
    if (input == INPUT_IDLE) then
        local move_track = context:getTrack(STATIC_TRACK_LINE, MOVE_TRACK)
        if (USE_RUN_TRANSITION_CLIPS and context:hasAnimation(CLIP_RUN_END)) then
            context:runAnimation(CLIP_RUN_END, move_track, true, PLAY_ONCE_STOP, STATE_BLEND_TIME)
        end
        return this.movement_states.idle
    end
    if (input == INPUT_WALK) then
        local move_track = context:getTrack(STATIC_TRACK_LINE, MOVE_TRACK)
        if (USE_RUN_TRANSITION_CLIPS and context:hasAnimation(CLIP_RUN_END)) then
            context:runAnimation(CLIP_RUN_END, move_track, true, PLAY_ONCE_STOP, STATE_BLEND_TIME)
        end
        return this.movement_states.walk
    end
end

function movement_states.walk.entry(this, context)
    play_loop_track(context, BASE_TRACK, CLIP_WALK, CLIP_STATIC_IDLE, STATE_BLEND_TIME)
end

function movement_states.walk.update(this, context)
    local track = context:getTrack(STATIC_TRACK_LINE, BASE_TRACK)
    if (context:isStopped(track) or context:isHolding(track)) then
        play_loop_track(context, BASE_TRACK, CLIP_WALK, CLIP_STATIC_IDLE, STATE_BLEND_TIME)
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
