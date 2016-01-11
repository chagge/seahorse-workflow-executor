---
layout: documentation
displayTitle: Seahorse Bundled Image
docTab: bundled_image_overview
title: Seahorse Bundled Image
includeTechnicalOverviewMenu: true
description: Seahorse Bundled Image
---


**Table of Contents**

* Table of Contents
{:toc}

## Overview

Seahorse is an <a target="_blank" href="http://spark.apache.org">Apache Spark</a>
platform that allows you to create Spark applications using web-based, interactive user interface.
Seahorse bundled image is a virtual machine containing all necessary components to easily run Seahorse.

<img class="img-responsive" src="./img/bundled_image_overview.png" />

## Run Seahorse Bundled Image

### Requirements
* <a target="_blank" href="https://www.vagrantup.com/">Vagrant</a> (tested on version 1.8.1)
* <a target="_blank" href="https://www.virtualbox.org/">VirtualBox</a> (tested on version 5.0.10, there is an issue with version 5.0.12)
* <a target="_blank" href="https://www.google.com/chrome/">Google Chrome</a> (version >= 41)

Seahorse virtual machine requires several ports to be free on user host to run as expected:

    [8000, 9080, 8080, 15674, 61613, 15672, 8888].

### Download Seahorse Bundled Image

Vagrantfile describing image containing all necessary components of Seahorse can be downloaded from
[Downloads page](/downloads.html).
Seahorse bundled image is based on <a target="_blank" href="http://www.ubuntu.com/">Ubuntu</a> and contains Spark distribution in it.
Currently in the Seahorse community edition there is no possibility to connect to an external Spark cluster.

### Run Command
To run Seahorse bundled image you have to navigate to the directory where you downloaded the Vagrantfile and execute:

    vagrant up

After that you can navigate to <a target="_blank" href="http://localhost:8000">http://localhost:8000</a>
and use Seahorse Editor.

### Shutdown Command
To stop Seahorse bundled image you need to execute:

    vagrant halt

## Making local files available in Seahorse
By default Vagrant mounts host directory where virtual machine was started to `/vagrant` directory on virtual machine.
If you want to use data sets stored as local files you need to copy them to the directory where you started your virtual machine.

Example:

    cp /path/to/the/dataset/data.csv /path/to/the/dir/where/vagrantfile/is/

<img class="img-responsive" src="./img/file_param.png" />

## Troubleshooting
If something went wrong during Seahorse exploration, please let us know about faulty behaviour using
"Feedback" option in Seahorse Editor and restart your Seahorse:

    vagrant reload

## Replacing Seahorse Bundled Image With the Latest Version
If you want to replace your Seahorse with the newest version you need to:

    # stop currently running Seahorse
    vagrant halt
    # remove current seahorse box
    vagrant box remove seahorse-vm
    # remove older Vagrantfile
    rm Vagrantfile
    #get newest Vagrantfile (please check Downloads page)
    wget http://path.to.the.newest.vagrantfile
    #start newest Seahorse
    vagrant up