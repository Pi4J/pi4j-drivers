package com.pi4j.drivers.hat.waveshare;

import com.pi4j.context.Context;
import com.pi4j.drivers.input.GameController;

/**
 * Creates the controller shared between various Waveshare display hats
 */
class JoystickHatGameControllerFactory {

    static GameController create(Context pi4j) {
        GameController.Builder builder = new GameController.Builder(pi4j);
        builder.addGndSwitch(GameController.Key.KEY_1, 21)
                .addGndSwitch(GameController.Key.KEY_2, 20)
                .addGndSwitch(GameController.Key.KEY_3, 16)
                .addGndSwitch(GameController.Key.UP, 6)
                .addGndSwitch(GameController.Key.DOWN, 19)
                .addGndSwitch(GameController.Key.LEFT, 5)
                .addGndSwitch(GameController.Key.RIGHT, 26)
                .addGndSwitch(GameController.Key.CENTER, 13);
        return builder.build();
    }
}
