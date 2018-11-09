#! /bin/bash

image_name=nfsrods_test_image
branch_name='4-2-stable'

docker build -t $image_name \
             --build-arg irods_branch=$branch_name \
             --build-arg icommands_branch=$branch_name \
             .
