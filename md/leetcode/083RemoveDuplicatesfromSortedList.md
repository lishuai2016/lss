# 83. Remove Duplicates from Sorted List

- [remove-duplicates-from-sorted-list](https://leetcode-cn.com/problems/remove-duplicates-from-sorted-list/)


## 描述
```
给定一个排序链表，删除所有重复的元素，使得每个元素只出现一次。

示例 1:
输入: 1->1->2
输出: 1->2

示例 2:
输入: 1->1->2->3->3
输出: 1->2->3
```




## 思路
拿当前值和下一个节点的值比较是否相等，不相等的话指针后移；相等的话，修改当前节点执行下下个节点，游标指针不移动


## 题解
```java
class Solution {
    public ListNode deleteDuplicates(ListNode head) {
        //思路：遍历发现当前节点和后继节点重复，修改删除下一节点
        ListNode cur = head;
        while (cur != null) {
            ListNode next = cur.next;
            //当前节点的值和后继节点的值一样，删除当前节点的后继节点
            if (next != null && next.val == cur.val) {
               cur.next = next.next;
            } else {
                cur = next;
            }
        }
        return head;
    }
}

```