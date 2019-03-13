package org.reflections.repeatable;

/**
 * Classfile /Users/honwhywang/github/reflections/target/test-classes/org/reflections/repeatable/MultiName.class
 *   Last modified 2019-3-13; size 471 bytes
 *   MD5 checksum fb5f9f4fa77213e01322598ad6727f98
 *   Compiled from "MultiName.java"
 * public class org.reflections.repeatable.MultiName
 *   minor version: 0
 *   major version: 52
 *   flags: ACC_PUBLIC, ACC_SUPER
 * Constant pool:
 *    #1 = Methodref          #3.#20         // java/lang/Object."<init>":()V
 *    #2 = Class              #21            // org/reflections/repeatable/MultiName
 *    #3 = Class              #22            // java/lang/Object
 *    #4 = Utf8               <init>
 *    #5 = Utf8               ()V
 *    #6 = Utf8               Code
 *    #7 = Utf8               LineNumberTable
 *    #8 = Utf8               LocalVariableTable
 *    #9 = Utf8               this
 *   #10 = Utf8               Lorg/reflections/repeatable/MultiName;
 *   #11 = Utf8               SourceFile
 *   #12 = Utf8               MultiName.java
 *   #13 = Utf8               RuntimeVisibleAnnotations
 *   #14 = Utf8               Lorg/reflections/repeatable/Names;
 *   #15 = Utf8               value
 *   #16 = Utf8               Lorg/reflections/repeatable/Name;
 *   #17 = Utf8               name
 *   #18 = Utf8               foo
 *   #19 = Utf8               bar
 *   #20 = NameAndType        #4:#5          // "<init>":()V
 *   #21 = Utf8               org/reflections/repeatable/MultiName
 *   #22 = Utf8               java/lang/Object
 * {
 *   public org.reflections.repeatable.MultiName();
 *     descriptor: ()V
 *     flags: ACC_PUBLIC
 *     Code:
 *       stack=1, locals=1, args_size=1
 *          0: aload_0
 *          1: invokespecial #1                  // Method java/lang/Object."<init>":()V
 *          4: return
 *       LineNumberTable:
 *         line 5: 0
 *       LocalVariableTable:
 *         Start  Length  Slot  Name   Signature
 *             0       5     0  this   Lorg/reflections/repeatable/MultiName;
 * }
 * SourceFile: "MultiName.java"
 * RuntimeVisibleAnnotations:
 *   0: #14(#15=[@#16(#17=s#18),@#16(#17=s#19)])
 *
 * two @name was compiled to one @NAMEs Array with two elements
 */
@Name(name = "foo")
@Name(name = "bar")
public class MultiName {
}
