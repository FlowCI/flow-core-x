set +e

echo "\
deb http://mirrors.cloud.aliyuncs.com/ubuntu/ trusty main restricted universe multiverse\n\
deb http://mirrors.cloud.aliyuncs.com/ubuntu/ trusty-security main restricted universe multiverse\n\
deb http://mirrors.cloud.aliyuncs.com/ubuntu/ trusty-updates main restricted universe multiverse\n\
deb http://mirrors.cloud.aliyuncs.com/ubuntu/ trusty-proposed main restricted universe multiverse\n\
deb http://mirrors.cloud.aliyuncs.com/ubuntu/ trusty-backports main restricted universe multiverse\n\
deb-src http://mirrors.cloud.aliyuncs.com/ubuntu/ trusty main restricted universe multiverse\n\
deb-src http://mirrors.cloud.aliyuncs.com/ubuntu/ trusty-security main restricted universe multiverse\n\
deb-src http://mirrors.cloud.aliyuncs.com/ubuntu/ trusty-updates main restricted universe multiverse\n\
deb-src http://mirrors.cloud.aliyuncs.com/ubuntu/ trusty-proposed main restricted universe multiverse\n\
deb-src http://mirrors.cloud.aliyuncs.com/ubuntu/ trusty-backports main restricted universe multiverse\n\
deb http://mirrors.aliyun.com/ubuntu/ trusty main restricted universe multiverse\n\
deb http://mirrors.aliyun.com/ubuntu/ trusty-security main restricted universe multiverse\n\
deb http://mirrors.aliyun.com/ubuntu/ trusty-updates main restricted universe multiverse\n\
deb http://mirrors.aliyun.com/ubuntu/ trusty-proposed main restricted universe multiverse\n\
deb http://mirrors.aliyun.com/ubuntu/ trusty-backports main restricted universe multiverse\n\
deb-src http://mirrors.aliyun.com/ubuntu/ trusty main restricted universe multiverse\n\
deb-src http://mirrors.aliyun.com/ubuntu/ trusty-security main restricted universe multiverse\n\
deb-src http://mirrors.aliyun.com/ubuntu/ trusty-updates main restricted universe multiverse\n\
deb-src http://mirrors.aliyun.com/ubuntu/ trusty-proposed main restricted universe multiverse\n\
deb-src http://mirrors.aliyun.com/ubuntu/ trusty-backports main restricted universe multiverse\n\
" | tee /etc/apt/sources.list

dpkg --add-architecture i386 && apt-get update && \
  apt-get install -y bash git-core software-properties-common vim wget curl telnet python unzip \
  make gcc g++ libfreetype6 libfontconfig libncurses5:i386 libstdc++6:i386 python-dev jq connect-proxy \
  zlib1g:i386 language-pack-en bison mercurial binutils build-essential
