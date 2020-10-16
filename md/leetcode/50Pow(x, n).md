# 50. Pow(x, n)


https://leetcode-cn.com/problems/powx-n/




## 题目描述

实现 pow(x, n) ，即计算 x 的 n 次幂函数。

示例 1:

输入: 2.00000, 10

输出: 1024.00000

示例 2:

输入: 2.10000, 3

输出: 9.26100

示例 3:

输入: 2.00000, -2

输出: 0.25000

解释: 2-2 = 1/22 = 1/4 = 0.25

说明:

- -100.0 < x < 100.0
- n 是 32 位有符号整数，其数值范围是 [−231, 231 − 1] 。

## 思路

https://leetcode-cn.com/problems/powx-n/solution/powx-n-by-leetcode-solution/

- 思路1：暴力递归或者迭代

- 思路2：采用每次即使n/2，然后把中间结果相乘，n为奇数时再额外乘以一个

## 题解

> 思路1

```java
class Solution {
    //迭代（超时）
    public double myPow(double x, int n) {
        int k = Math.abs(n);
        double res = 1.0;
        while (k > 0) {
            res = res * x;
            k--;
        }
        return n < 0 ? 1 / res: res;
    }
    //递归(栈溢出)
    public double myPow(double x, int n) {
        if (n == 0) {
            return 1.0;
        }
        if (n == 1) {
            return x;
        }
        if (n < 0) {
            return 1 / myPow(x,Math.abs(n));
        }
        return x * myPow(x,n - 1);
    }
}
```

备注：存在的问题是超时


> 思路2

```java
class Solution {
    public double myPow(double x, int n) {
        return n > 0 ? quick(x,n) : 1.0 / quick(x,-n);
    }
    //备注，这里需要考虑int溢出的问题，这里使用long
    public double quick(double x,long k) {
        if (k == 0) {
            return 1.0;
        }
        double temp = quick(x,k / 2);
        return k % 2 == 0 ? temp * temp : temp * temp * x;
    }
}
```