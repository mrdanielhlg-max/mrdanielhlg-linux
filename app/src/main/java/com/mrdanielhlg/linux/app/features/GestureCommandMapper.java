package com.mrdanielhlg.linux.app.features;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * GestureCommandMapper - Map touch gestures to terminal commands/actions.
 *
 * Features:
 *  - Swipe left/right/up/down from edges → configurable action
 *  - Two-finger swipe → separate actions
 *  - Double tap → configurable action
 *  - Long press → configurable action
 *  - Per-session gesture profiles
 *
 * Default gestures:
 *  - Swipe right  → previous session
 *  - Swipe left   → next session
 *  - Swipe down (2 fingers) → scroll to top
 *  - Swipe up (2 fingers)   → scroll to bottom
 *  - Double tap   → toggle keyboard
 *  - Long press   → context menu
 *  - Pinch out    → increase font size
 *  - Pinch in     → decrease font size
 */
public class GestureCommandMapper {

    private static final String PREFS_NAME = "mrdanielhlg_gestures";
    private static final String KEY_GESTURES = "gestures_json";

    // Gesture identifiers
    public static final String GESTURE_SWIPE_LEFT = "swipe_left";
    public static final String GESTURE_SWIPE_RIGHT = "swipe_right";
    public static final String GESTURE_SWIPE_UP = "swipe_up";
    public static final String GESTURE_SWIPE_DOWN = "swipe_down";
    public static final String GESTURE_SWIPE_LEFT_2F = "swipe_left_2finger";
    public static final String GESTURE_SWIPE_RIGHT_2F = "swipe_right_2finger";
    public static final String GESTURE_SWIPE_UP_2F = "swipe_up_2finger";
    public static final String GESTURE_SWIPE_DOWN_2F = "swipe_down_2finger";
    public static final String GESTURE_DOUBLE_TAP = "double_tap";
    public static final String GESTURE_LONG_PRESS = "long_press";
    public static final String GESTURE_PINCH_IN = "pinch_in";
    public static final String GESTURE_PINCH_OUT = "pinch_out";

    // Action types
    public static final String ACTION_NEXT_SESSION = "next_session";
    public static final String ACTION_PREV_SESSION = "prev_session";
    public static final String ACTION_NEW_SESSION = "new_session";
    public static final String ACTION_CLOSE_SESSION = "close_session";
    public static final String ACTION_TOGGLE_KEYBOARD = "toggle_keyboard";
    public static final String ACTION_SCROLL_TOP = "scroll_top";
    public static final String ACTION_SCROLL_BOTTOM = "scroll_bottom";
    public static final String ACTION_CONTEXT_MENU = "context_menu";
    public static final String ACTION_FONT_BIGGER = "font_bigger";
    public static final String ACTION_FONT_SMALLER = "font_smaller";
    public static final String ACTION_COPY = "copy";
    public static final String ACTION_PASTE = "paste";
    public static final String ACTION_CLEAR_SCREEN = "clear_screen";
    public static final String ACTION_QUICK_SCRIPTS = "open_quick_scripts";
    public static final String ACTION_SYSTEM_MONITOR = "open_system_monitor";
    public static final String ACTION_COMMAND = "run_command"; // custom command
    public static final String ACTION_NONE = "none";

    public static class GestureAction {
        public String actionType;
        public String customCommand; // used if actionType == ACTION_COMMAND
        public String label;

        public GestureAction(String actionType, String label) {
            this.actionType = actionType;
            this.label = label;
        }

        public GestureAction(String customCommand) {
            this.actionType = ACTION_COMMAND;
            this.customCommand = customCommand;
            this.label = "Run: " + customCommand;
        }
    }

    private final SharedPreferences prefs;
    private final Map<String, GestureAction> gestureMap = new HashMap<>();

    public GestureCommandMapper(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadDefaults();
        load();
    }

    public GestureAction getAction(String gesture) {
        GestureAction a = gestureMap.get(gesture);
        return a != null ? a : new GestureAction(ACTION_NONE, "No action");
    }

    public void setAction(String gesture, String actionType, String customCommand) {
        GestureAction a;
        if (ACTION_COMMAND.equals(actionType) && customCommand != null) {
            a = new GestureAction(customCommand);
        } else {
            a = new GestureAction(actionType, getLabelForAction(actionType));
        }
        gestureMap.put(gesture, a);
        save();
    }

    public void resetToDefaults() {
        gestureMap.clear();
        loadDefaults();
        save();
    }

    public Map<String, GestureAction> getAllMappings() {
        return new HashMap<>(gestureMap);
    }

    public String getLabelForAction(String actionType) {
        switch (actionType) {
            case ACTION_NEXT_SESSION: return "Next Session";
            case ACTION_PREV_SESSION: return "Previous Session";
            case ACTION_NEW_SESSION: return "New Session";
            case ACTION_CLOSE_SESSION: return "Close Session";
            case ACTION_TOGGLE_KEYBOARD: return "Toggle Keyboard";
            case ACTION_SCROLL_TOP: return "Scroll to Top";
            case ACTION_SCROLL_BOTTOM: return "Scroll to Bottom";
            case ACTION_CONTEXT_MENU: return "Context Menu";
            case ACTION_FONT_BIGGER: return "Font Size +";
            case ACTION_FONT_SMALLER: return "Font Size -";
            case ACTION_COPY: return "Copy";
            case ACTION_PASTE: return "Paste";
            case ACTION_CLEAR_SCREEN: return "Clear Screen";
            case ACTION_QUICK_SCRIPTS: return "Quick Scripts";
            case ACTION_SYSTEM_MONITOR: return "System Monitor";
            case ACTION_NONE: return "No Action";
            default: return actionType;
        }
    }

    public String[] getAllActionTypes() {
        return new String[]{
            ACTION_NEXT_SESSION, ACTION_PREV_SESSION, ACTION_NEW_SESSION,
            ACTION_CLOSE_SESSION, ACTION_TOGGLE_KEYBOARD, ACTION_SCROLL_TOP,
            ACTION_SCROLL_BOTTOM, ACTION_CONTEXT_MENU, ACTION_FONT_BIGGER,
            ACTION_FONT_SMALLER, ACTION_COPY, ACTION_PASTE, ACTION_CLEAR_SCREEN,
            ACTION_QUICK_SCRIPTS, ACTION_SYSTEM_MONITOR, ACTION_COMMAND, ACTION_NONE
        };
    }

    private void loadDefaults() {
        gestureMap.put(GESTURE_SWIPE_RIGHT, new GestureAction(ACTION_PREV_SESSION, "Previous Session"));
        gestureMap.put(GESTURE_SWIPE_LEFT, new GestureAction(ACTION_NEXT_SESSION, "Next Session"));
        gestureMap.put(GESTURE_SWIPE_DOWN_2F, new GestureAction(ACTION_SCROLL_TOP, "Scroll to Top"));
        gestureMap.put(GESTURE_SWIPE_UP_2F, new GestureAction(ACTION_SCROLL_BOTTOM, "Scroll to Bottom"));
        gestureMap.put(GESTURE_DOUBLE_TAP, new GestureAction(ACTION_TOGGLE_KEYBOARD, "Toggle Keyboard"));
        gestureMap.put(GESTURE_LONG_PRESS, new GestureAction(ACTION_CONTEXT_MENU, "Context Menu"));
        gestureMap.put(GESTURE_PINCH_OUT, new GestureAction(ACTION_FONT_BIGGER, "Font Size +"));
        gestureMap.put(GESTURE_PINCH_IN, new GestureAction(ACTION_FONT_SMALLER, "Font Size -"));
        gestureMap.put(GESTURE_SWIPE_LEFT_2F, new GestureAction(ACTION_QUICK_SCRIPTS, "Quick Scripts"));
        gestureMap.put(GESTURE_SWIPE_RIGHT_2F, new GestureAction(ACTION_SYSTEM_MONITOR, "System Monitor"));
        gestureMap.put(GESTURE_SWIPE_UP, new GestureAction(ACTION_NONE, "No Action"));
        gestureMap.put(GESTURE_SWIPE_DOWN, new GestureAction(ACTION_NONE, "No Action"));
    }

    private void save() {
        try {
            JSONObject obj = new JSONObject();
            for (Map.Entry<String, GestureAction> e : gestureMap.entrySet()) {
                JSONObject actionObj = new JSONObject();
                actionObj.put("type", e.getValue().actionType);
                actionObj.put("cmd", e.getValue().customCommand != null ? e.getValue().customCommand : "");
                obj.put(e.getKey(), actionObj);
            }
            prefs.edit().putString(KEY_GESTURES, obj.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void load() {
        try {
            String json = prefs.getString(KEY_GESTURES, null);
            if (json == null) return;
            JSONObject obj = new JSONObject(json);
            java.util.Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String gesture = keys.next();
                JSONObject actionObj = obj.getJSONObject(gesture);
                String type = actionObj.getString("type");
                String cmd = actionObj.optString("cmd", "");
                GestureAction a;
                if (ACTION_COMMAND.equals(type) && !cmd.isEmpty()) {
                    a = new GestureAction(cmd);
                } else {
                    a = new GestureAction(type, getLabelForAction(type));
                }
                gestureMap.put(gesture, a);
            }
        } catch (Exception ignored) {}
    }
}
