/*
 * Copyright (c) 2014, dags_ <dags@dags.me>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package me.dags.daflight.player;

import me.dags.daflight.LiteModDaFlight;
import me.dags.daflight.input.KeybindHandler;
import me.dags.daflight.input.MovementHandler;
import me.dags.daflight.input.binds.KeyBinds;
import me.dags.daflight.messaging.PacketData;
import me.dags.daflight.messaging.PluginChannelUtil;
import me.dags.daflight.minecraft.MCGame;
import me.dags.daflight.player.controller.CineFlightController;
import me.dags.daflight.player.controller.FlightController;
import me.dags.daflight.player.controller.IController;
import me.dags.daflight.player.controller.SprintController;
import me.dags.daflight.utils.Config;
import me.dags.daflight.utils.GlobalConfig;
import me.dags.daflight.utils.SpeedDefaults;

/**
 * @author dags_ <dags@dags.me>
 */

public class DaPlayer extends MCGame
{
    public static final KeyBinds KEY_BINDS = new KeyBinds();
    public static final DFPermissions DF_PERMISSIONS = new DFPermissions();

    public Speed sprintSpeed;
    public Speed flySpeed;

    public boolean flyModOn = false;
    public boolean sprintModOn = false;
    public boolean cineFlightOn = false;
    public boolean fullBrightOn = false;

    public Direction direction;
    public Vector movementVector;
    private IController controller;

    private boolean inMenus = true;
    private boolean wasFlying = false;
    private int softFallTicks = 0;

    public DaPlayer()
    {
        direction = new Direction();
        movementVector = new Vector();

        SpeedDefaults speedDefaults = SpeedDefaults.loadDefaults();

        flySpeed = new Speed().setMaxBaseSpeed(speedDefaults.getDefaultMaxBaseSpeed()).setMaxMultiplier(speedDefaults.getDefaultMultiplieer());
        flySpeed.setBaseSpeed(Config.getInstance().flySpeed);
        flySpeed.setMultiplier(Config.getInstance().flySpeedMult);

        sprintSpeed = new Speed().setMaxBaseSpeed(speedDefaults.getDefaultMaxBaseSpeed()).setMaxMultiplier(speedDefaults.getDefaultMultiplieer());
        sprintSpeed.setBaseSpeed(Config.getInstance().sprintSpeed);
        sprintSpeed.setMultiplier(Config.getInstance().sprintSpeedMult);
    }

    public void onGameJoin()
    {
        DF_PERMISSIONS.resetPermissions();
        flySpeed.setMaxSpeed(flySpeed.getMaxBaseSpeed() * flySpeed.getMaxMultiplier());
        sprintSpeed.setMaxSpeed(sprintSpeed.getMaxBaseSpeed() * sprintSpeed.getMaxMultiplier());
        PluginChannelUtil.dispatchPacket(PacketData.CONNECT);
    }

    public void tickUpdate()
    {
        if (getMinecraft().inGameHasFocus)
        {
            if (wasFlying && onSolidBlock())
            {
                wasFlying = false;
                softFallTicks = 5;
            }
            softFallTicks--;
        }
    }

    public void update()
    {
        if (getMinecraft().inGameHasFocus)
        {
            if (inMenus)
            {
                inMenus = false;
                KEY_BINDS.updateMovementKeys();
            }
            KeybindHandler.handleInput(this);
            if (isModOn() && controller != null)
            {
                MovementHandler.handleMovementInput(this);
                controller.input(movementVector);
            }
        }
        else
        {
            if (!inMenus)
            {
                inMenus = true;
            }
            if (isModOn() && controller != null)
            {
                controller.unFocused();
            }
        }
    }

    public void toggleFlight()
    {
        flyModOn = DF_PERMISSIONS.flyEnabled() && !flyModOn;
        toggleFlightCommon();
        if (!flyModOn && !Config.getInstance().speedIsToggle)
        {
            flySpeed.setBoost(false);
        }
        if (!flyModOn)
        {
            getPlayer().capabilities.isFlying = false;
            getPlayer().sendPlayerAbilities();
            if (cineFlightOn)
            {
                getGameSettings().smoothCamera = false;
            }
        }
        if (flyModOn && cineFlightOn)
        {
            getGameSettings().smoothCamera = true;
        }
        notifyServer();
    }

    public void toggleCineFlight()
    {
        if (flyModOn)
        {
            cineFlightOn = !cineFlightOn;
            getMinecraft().gameSettings.smoothCamera = cineFlightOn;
        }
        toggleFlightCommon();
    }

    public void toggleSprint()
    {
        sprintModOn = DF_PERMISSIONS.sprintEnabled() && !sprintModOn;
        controller = getActiveController();
        if (!sprintModOn && !Config.getInstance().speedIsToggle)
        {
            sprintSpeed.setBoost(false);
        }
        notifyServer();
    }

    public void toggleSpeedModifier()
    {
        if (flyModOn)
        {
            flySpeed.toggleBoost();
        }
        else if (sprintModOn)
        {
            sprintSpeed.toggleBoost();
        }
    }

    private void toggleFlightCommon()
    {
        controller = getActiveController();
        if (!flyModOn && !cineFlightOn)
        {
            getPlayer().capabilities.isFlying = false;
            getPlayer().sendPlayerAbilities();
        }
    }

    private void notifyServer()
    {
        if (flyModOn || sprintModOn)
        {
            PluginChannelUtil.dispatchPacket(PacketData.MOD_ON);
            return;
        }
        PluginChannelUtil.dispatchPacket(PacketData.MOD_OFF);
    }

    public void toggleFullbright()
    {
        float brightness = 9999F;
        if (fullBrightOn || !DF_PERMISSIONS.fbEnabled())
        {
            fullBrightOn = false;
            brightness = GlobalConfig.getBrightness();
        }
        else
        {
            fullBrightOn = true;
            GlobalConfig.setBrightness(getMinecraft().gameSettings.gammaSetting);
            GlobalConfig.saveSettings();
        }
        getMinecraft().gameSettings.gammaSetting = brightness;
    }

    public void disableAll()
    {
        if (getMinecraft().inGameHasFocus)
        {
            if (flyModOn)
            {
                toggleFlight();
            }
            if (sprintModOn)
            {
                toggleSprint();
            }
            if (fullBrightOn)
            {
                toggleFullbright();
            }
        }
    }

    public void disableMovementMods()
    {
        if (flyModOn)
        {
            toggleFlight();
        }
        if (sprintModOn)
        {
            toggleSprint();
        }
        flySpeed.setBoost(false);
        sprintSpeed.setBoost(false);
        LiteModDaFlight.getHud().updateMsg();
    }

    public boolean softFallOn()
    {
        if (Config.getInstance().disabled || !DF_PERMISSIONS.noFallDamageEnabled())
        {
            return false;
        }
        if (flyModOn || sprintModOn)
        {
            return wasFlying = true;
        }
        return wasFlying || softFallTicks > 0 || (wasFlying = false);
    }

    private IController getActiveController()
    {
        return flyModOn && cineFlightOn ? new CineFlightController() : flyModOn ? new FlightController() : sprintModOn ? new SprintController() : null;
    }

    private boolean isModOn()
    {
        return flyModOn || cineFlightOn || sprintModOn;
    }

    public boolean is3DFlightOn()
    {
        return Config.getInstance().threeDFlight;
    }

    public double getSpeed()
    {
        if (flyModOn)
        {
            return flySpeed.getTotalSpeed();
        }
        else if (sprintModOn)
        {
            return sprintSpeed.getTotalSpeed();
        }
        return 1D;
    }

}
