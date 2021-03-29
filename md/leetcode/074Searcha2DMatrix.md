

<!-- TOC -->

- [1、题目描述](#1题目描述)
- [2、题解](#2题解)
    - [1、把二维数组当成一个有序的数组。时间复杂度log(M*N)  二分搜索的思路[最优]](#1把二维数组当成一个有序的数组时间复杂度logmn--二分搜索的思路最优)
    - [2、二分查找行和列定位](#2二分查找行和列定位)

<!-- /TOC -->

74搜索二维矩阵： https://leetcode-cn.com/problems/search-a-2d-matrix/


# 1、题目描述
编写一个高效的算法来判断 m x n 矩阵中，是否存在一个目标值。该矩阵具有如下特性：

每行中的整数从左到右按升序排列。

每行的第一个整数大于前一行的最后一个整数。

示例 1:

```
输入:
matrix = [
  [1,   3,  5,  7],
  [10, 11, 16, 20],
  [23, 30, 34, 50]
]
target = 3
输出: true
示例 2:

输入:
matrix = [
  [1,   3,  5,  7],
  [10, 11, 16, 20],
  [23, 30, 34, 50]
]
target = 13
输出: false
```




# 2、题解


## 1、把二维数组当成一个有序的数组。时间复杂度log(M*N)  二分搜索的思路[最优]
```java
class Solution {
    public boolean searchMatrix(int[][] matrix, int target) {
               if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return false;
        }
        int rows = matrix.length;
        int cols = matrix[0].length;
        int nums = rows * cols;

        int left = 0;
        int right = nums - 1;
        int index ;
        while (left <= right) {
            index =  (right + left ) / 2;
            //注意这里都是cols
            int row = index / cols;
            int col = index % cols;
            if (matrix[row][col] == target) {
                return true;
            } else if (matrix[row][col] > target) {
                right = index - 1;
            } else {
                left = index + 1;
            }
        }
        return false;
    }
}
```


## 2、二分查找行和列定位

```java
class Solution {
    public boolean searchMatrix(int[][] matrix, int target) {
        //先按照行二分，定位在哪一行，然后再行中定位列
        int row = matrix.length;
        int col = matrix[0].length;
        int left = 0;
        int right = row;
        while (left < right) {
            int mid = left + (right - left) / 2;
            if (matrix[mid][0] < target) {
                left = mid + 1;
            } else if (matrix[mid][0] == target) {
                return true;
            } else {
                right = mid;
            }
        }
        //需要判断当前行开头是否登录目标元素
        if (left >= 0 && left < row && matrix[left][0] == target) {
            return true;
        }
        //此时的left是大于target元素的下一行
        if (left > 0) {
            left--;
        }

        int low = 0;
        int high = col - 1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (matrix[left][mid] == target) {
                return true;
            } else if (matrix[left][mid] < target) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return false;
    }
}
```