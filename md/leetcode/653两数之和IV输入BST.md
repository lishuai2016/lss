653. 两数之和 IV - 输入 BST


- [653. 两数之和 IV - 输入 BST](https://leetcode-cn.com/problems/two-sum-iv-input-is-a-bst/)

```
给定一个二叉搜索树和一个目标结果，如果 BST 中存在两个元素且它们的和等于给定的目标结果，则返回 true。

案例 1:

输入: 
    5
   / \
  3   6
 / \   \
2   4   7

Target = 9

输出: True
 

案例 2:

输入: 
    5
   / \
  3   6
 / \   \
2   4   7

Target = 28

输出: False

```

思路：中序遍历，然后把遍历过的元素放到set中，然后判断k-node.val是否存在，存在的话就可以提前结束




```java
/**
 * Definition for a binary tree node.
 * public class TreeNode {
 *     int val;
 *     TreeNode left;
 *     TreeNode right;
 *     TreeNode(int x) { val = x; }
 * }
 */
class Solution {
    boolean found = false;//是否找到的标记
    public boolean findTarget(TreeNode root, int k) {
        if (root == null) return false;
        Set<Integer> set = new HashSet<>();
        dfs(root,set,k);
        return found;
    }
    public void dfs(TreeNode root,Set<Integer> set,int k) {
        if (!found) {//找到了的话可以提前结束
            if (root == null) return;
            dfs(root.left,set,k);
            if (set.contains(k - root.val)) {//判断是否找到
                found = true;
                return;
            }
            set.add(root.val);
            dfs(root.right,set,k);
        } 
    }
}

```
