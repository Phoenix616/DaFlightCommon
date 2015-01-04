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

package me.dags.daflight.player.controller;

import me.dags.daflight.LiteModDaFlight;
import me.dags.daflight.minecraft.MCGame;
import me.dags.daflight.player.DaPlayer;
import me.dags.daflight.player.Direction;
import me.dags.daflight.player.Vector;
import me.dags.daflight.utils.Config;

public class CineFlightController extends MCGame implements IController
{

    private Config c = Config.getInstance();
    private Double up;
    private Double down;
    private Double forward;
    private Double back;
    private Double left;
    private Double right;

    private Direction d;

    public CineFlightController()
    {
        d = new Direction();
        up = 0D;
        down = 0D;
        forward = 0D;
        back = 0D;
        left = 0D;
        right = 0D;
    }

    @Override
    public void input(Vector v)
    {
        d.update((double) getPlayer().rotationYaw);

        double x = 0D;
        double y = 0D;
        double z = 0D;

        double pitch = getPlayer().rotationPitch;

        // FORWARD
        if (DaPlayer.KEY_BINDS.forward.keyHeld())
        {
            back = dec(back);
            if (back <= 0)
            {
                y = -1.1 * (pitch / 90);
                forward = inc(forward);
            }
            x += d.getX() * forward;
            z += d.getZ() * forward;
        }
        else
        {
            forward = dec(forward);
            x += d.getX() * forward;
            z += d.getZ() * forward;
            if (back <= 0)
                y = -1.1 * (pitch / 90);
        }
        // BACK
        if (DaPlayer.KEY_BINDS.backward.keyHeld())
        {
            forward = dec(forward);
            if (forward <= 0)
            {
                y = 1.1 * (pitch / 90);
                back = inc(back);
            }
            x += -d.getX() * back;
            z += -d.getZ() * back;
        }
        else
        {
            back = dec(back);
            x += -d.getX() * back;
            z += -d.getZ() * back;
        }
        // LEFT
        if (DaPlayer.KEY_BINDS.left.keyHeld())
        {
            right = dec(right);
            left = inc(left);
            x += d.getZ() * c.lrModifier * left;
            z += -d.getX() * c.lrModifier * left;
        }
        else
        {
            left = dec(left);
            x += d.getZ() * c.lrModifier * left;
            z += -d.getX() * c.lrModifier * left;
        }
        // RIGHT
        if (DaPlayer.KEY_BINDS.right.keyHeld())
        {
            left = dec(left);
            right = inc(right);
            x += -d.getZ() * c.lrModifier * right;
            z += d.getX() * c.lrModifier * right;
        }
        else
        {
            right = dec(right);
            x += -d.getZ() * c.lrModifier * right;
            z += d.getX() * c.lrModifier * right;
        }
        // UP
        if (DaPlayer.KEY_BINDS.flyUp.keyHeld())
        {
            up = inc(up);
            y += 1.1D * up;
        }
        else
        {
            up = dec(up);
            y += 1.1D * up;
        }
        // DOWN
        if (DaPlayer.KEY_BINDS.flyDown.keyHeld())
        {
            down = inc(down);
            y += -1.1D * down;
        }
        else
        {
            down = dec(down);
            y += -1.1D * down;
        }

        if (x == 0 & y == 0 & z == 0)
        {
            getPlayer().setVelocity(getPlayer().motionX * c.flySmoothing, getPlayer().motionY, getPlayer().motionZ * c.flySmoothing);
        }
        else
        {
            double speed = LiteModDaFlight.DAPLAYER.getSpeed();
            getPlayer().setVelocity(x * speed, y * speed, z * speed);
        }
    }

    @Override
    public void unFocused()
    {
        getPlayer().setVelocity(getPlayer().motionX * c.flySmoothing, 0, getPlayer().motionZ * c.flySmoothing);
    }

    private Double dec(Double d)
    {
        if (d > 0)
            d -= 0.001;
        if (d < 0)
            d = 0D;
        return d;
    }

    private Double inc(Double d)
    {
        if (d < 1)
            d += 0.001;
        if (d < 0)
            d = 0D;
        return d;
    }
}
