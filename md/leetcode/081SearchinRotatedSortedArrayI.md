

<!-- TOC -->

- [1、题目描述](#1题目描述)
- [2、题解](#2题解)

<!-- /TOC -->


81搜索旋转排序数组II : https://leetcode-cn.com/problems/search-in-rotated-sorted-array-ii/





# 1、题目描述

假设按照升序排序的数组在预先未知的某个点上进行了旋转。

( 例如，数组 [0,0,1,2,2,5,6] 可能变为 [2,5,6,0,0,1,2] )。

编写一个函数来判断给定的目标值是否存在于数组中。若存在返回 true，否则返回 false。

示例 1:

输入: nums = [2,5,6,0,0,1,2], target = 0
输出: true
示例 2:

输入: nums = [2,5,6,0,0,1,2], target = 3
输出: false

进阶:

- 这是 搜索旋转排序数组 的延伸题目，本题中的 nums  可能包含重复元素。
- 这会影响到程序的时间复杂度吗？会有怎样的影响，为什么？



# 2、题解


```java
class Solution {
    //[1,1,1,1,1,1,1,1,1,1,1,1,1,2,1,1,1,1,1]  没法再像之前一样找到旋转点了
    public boolean search(int[] nums, int target) {
        boolean res = false;
        int left  = 0; 
        int right = nums.length - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] == target) {
                return true;
            }
            //1、没法判断是前部分有序还是后部分有序
            if (nums[mid] == nums[left]) {
                left++;
                continue;
            }
            //2、前半段有序
            if (nums[left] < nums[mid]) {
                //在前半段找
                if (nums[left] <= target && target < nums[mid]) {
                    right = mid - 1;
                } else {
                    left = mid + 1;
                }
            } else {
            //3、后半段有序
                //在后半段找
                if (nums[mid] < target && target <= nums[right]) {
                    left = mid + 1;
                } else {
                    right = mid - 1;
                }
            }
        }
        return res;
    }
}
```

