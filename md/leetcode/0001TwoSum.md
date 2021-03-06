
<!-- TOC -->

- [1、题目描述](#1题目描述)
- [2、思路](#2思路)
- [3、题解](#3题解)

<!-- /TOC -->

[two-sum](https://leetcode-cn.com/problems/two-sum/)

# 1、题目描述

给定一个整数数组 nums 和一个目标值 target，请你在该数组中找出和为目标值的那 两个 整数，并返回他们的数组下标。

你可以假设每种输入只会对应一个答案。但是，你不能重复利用这个数组中同样的元素。

示例:

给定 nums = [2, 7, 11, 15], target = 9

因为 nums[0] + nums[1] = 2 + 7 = 9

所以返回 [0, 1]

# 2、思路

- 思路1：直接两次循环遍历，时间复杂度N*N

- 思路2：借助hashmap，时间复杂度为N，空间复杂度N

# 3、题解

> 思路1

```java
public class Solution {
    public int[] twoSum(int[] nums, int target) {
        int[] res = new int[2];
        for (int i = 0;i < nums.length;i++) {
            for (int j = i+1;j < nums.length;j++) {
                if (nums[i] + nums[j] == target) {
                    res[0] = i;
                    res[1] = j;
                }
            }
        }
        return res;
    }
}
```

> 思路2

```java
public class Solution {
    public int[] twoSum(int[] nums, int target) {
       int[] res = new int[2];
       Map<Integer,Integer> map = new HashMap<Integer,Integer>();
       for (int i = 0;i < nums.length;i++) {
           if(map.containsKey(target - nums[i])) {
               res[1] = i;
               res[0] = map.get(target-nums[i]);
               return res;
           }
           map.put(nums[i],i);
       }
       return res;
    }
}
```



