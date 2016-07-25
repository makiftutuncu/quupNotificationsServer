quup Notifications Server
=================================

Welcome to quup Notifications Server!

This is a server application for regularly checking notifications on https://quup.com and pushing them to registered clients. There is an Android client implemented here https://github.com/mehmetakiftutuncu/quupNotificationsAndroid.

It is currently deployed to https://quupnotifications.herokuapp.com.

Technical Details
--------------
quup Notifications Server application is developed using [Play Framework](https://www.playframework.com/) and [Scala](http://www.scala-lang.org/). There is a simple MySQL database provided by Heroku for persistence. The application utilizes [Akka](http://akka.io/) actors and [WS](https://www.playframework.com/documentation/latest/ScalaWS) and [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging) for pushing to devices.

Privacy
--------------
quup Notifications Server does not read or store your personal information in any way. The only information kept is a user's session on quup in order to be able to pull notifications from quup. Your notifications are retrieved from quup and sent directly to your device.

Disclaimer
--------------
This application is developed voluntarily and independently of quup team to serve quup users.

For more information about quup, you may visit https://quup.com/about.

License
--------------
quup Notifications Server is licensed under Apache License Version 2.0.

```
Copyright (C) 2015 Mehmet Akif Tütüncü

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
