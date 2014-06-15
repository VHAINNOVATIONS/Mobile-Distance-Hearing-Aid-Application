// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;

public final class ActivityHelpers
{
    private ActivityHelpers()
    {
    }

    public static <T extends Fragment> T attach(Activity activity, Class<T> fragmentType, String tag)
    {
        return attach(activity, 0, fragmentType, tag);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Fragment> T attach(Activity activity, int containerId, Class<T> fragmentType, String tag)
    {
        if (activity == null)
        {
            throw new IllegalArgumentException("activity is null");
        }
        if (fragmentType == null)
        {
            throw new IllegalArgumentException("fragmentType is null");
        }
        if (tag == null)
        {
            throw new IllegalArgumentException("tag is null");
        }

        T result;

        FragmentManager manager = activity.getFragmentManager();
        Fragment fragment = manager.findFragmentByTag(tag);

        if (fragment == null)
        {
            try
            {
                result = fragmentType.newInstance();
            }
            catch (InstantiationException e)
            {
                throw new IllegalArgumentException("fragmentType cannot be instantiated (default constructor is not visible)", e);
            }
            catch (IllegalAccessException e)
            {
                throw new IllegalArgumentException("fragmentType cannot be instantiated (instance could not be created)", e);
            }

            manager.beginTransaction().add(containerId, result, tag).commit();
        }
        else
        {
            if (!fragmentType.isInstance(fragment))
            {
                throw new IllegalArgumentException("Different fragmentType for tag");
            }

            result = (T)fragment;
        }

        return result;
    }
}
