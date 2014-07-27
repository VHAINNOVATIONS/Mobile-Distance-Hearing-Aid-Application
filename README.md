# Phonak

Innovation 291

## Description

A smartphone app prototype that enables patients to receive follow up fittings of their hearing instruments.

## Prerequisites

* Android 4.4 device, with Bluetooth
* Android Studio IDE (https://developer.android.com/sdk/installing/studio.html)
* Phonak fitting software, fitting device and hearing instruments (http://phonak.com/)
* Yaler relay service, dedicated instance (https://yaler.net/)
* HockeyApp deployment service (http://hockeyapp.net/)

## Building the app

* Start Android Studio
* Open Project > Browse "Phonak/Difian" > Open
* Configure pref_relay_region_values in arrays.xml
* Configure HOCKEY_KEY in MainActivity.java
* Build > Make Project

## Running the app

* Run > Debug "DifianApp"