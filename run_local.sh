#!/bin/bash

sbt "~run -Drun.mode=Dev -Dhttp.port=9614 -Dapplication.router=testOnlyDoNotUseInAppConf.Routes -Ddeskpro.api-key=${DESKPRO_KEY} $*"
