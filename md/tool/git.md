

<!-- TOC -->

- [1.查看所有分支](#1查看所有分支)
- [2.查看当前使用分支](#2查看当前使用分支)
- [3.切换分支](#3切换分支)
- [4.clone指定分支代码](#4clone指定分支代码)
- [5.git clone 时显示Filename too long的解决办法](#5git-clone-时显示filename-too-long的解决办法)

<!-- /TOC -->




# 1.查看所有分支

git branch -a 


# 2.查看当前使用分支

git branch

# 3.切换分支

git checkout 分支名

# 4.clone指定分支代码

git clone -b b_f_4.1.1_20200615  https://github.com/lishuai2016/shardingsphere.git


使用参数-b 指定分支

# 5.git clone 时显示Filename too long的解决办法

在git bash中，运行下列命令： git config --global core.longpaths true

就可以解决该问题。

--global是该参数的使用范围，如果只想对本版本库设置该参数，只要在上述命令中去掉--global即可。

