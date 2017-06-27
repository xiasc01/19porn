package cn.droidlover.xdroid.demo.kit;

/**
 * Created by Administrator on 2017/2/14 0014.
 */

public class DES {
    private static final int[] IP = {
            58, 50, 42, 34, 26, 18, 10, 2,
            60, 52, 44, 36, 28, 20, 12, 4,
            62, 54, 46, 38, 30, 22, 14, 6,
            64, 56, 48, 40, 32, 24, 16, 8,
            57, 49, 41, 33, 25, 17, 9, 1,
            59, 51, 43, 35, 27, 19, 11, 3,
            61, 53, 45, 37, 29, 21, 13, 5,
            63, 55, 47, 39, 31, 23, 15, 7 };

    private static final int[] FP = {
            40, 8, 48, 16, 56, 24, 64, 32,
            39, 7, 47, 15, 55, 23, 63, 31,
            38, 6, 46, 14, 54, 22, 62, 30,
            37, 5, 45, 13, 53, 21, 61, 29,
            36, 4, 44, 12, 52, 20, 60, 28,
            35, 3, 43, 11, 51, 19, 59, 27,
            34, 2, 42, 10, 50, 18, 58, 26,
            33, 1, 41, 9, 49, 17, 57, 25 };

    private static final int[] KP = {
            57, 49, 41, 33, 25, 17, 9, 1,
            58, 50, 42, 34, 26, 18, 10, 2,
            59, 51, 43, 35, 27, 19, 11, 3,
            60, 52, 44, 36, 63, 55, 47, 39,
            31, 23, 15, 7, 62, 54, 46, 38,
            30, 22, 14, 6, 61, 53, 45, 37,
            29, 21, 13, 5, 28, 20, 12, 4 };

    private static final int[] KM = {
             1, 1, 2, 2, 2, 2, 2, 2,
             1, 2, 2, 2, 2, 2, 2, 1 };

    private static final int[] CP = {
            14, 17, 11, 24, 1, 5, 3, 28,
            15, 6, 21, 10, 23, 19, 12, 4,
            26, 8, 16, 7, 27, 20, 13, 2,
            41, 52, 31, 37, 47, 55, 30, 40,
            51, 45, 33, 48, 44, 49, 39, 56,
            34, 53, 46, 42, 50, 36, 29, 32 };

    private static final int[] EP = {
            32, 1, 2, 3, 4, 5, 4, 5,
            6, 7, 8, 9, 8, 9, 10, 11,
            12, 13, 12, 13, 14, 15, 16, 17,
            16, 17, 18, 19, 20, 21, 20, 21,
            22, 23, 24, 25, 24, 25, 26, 27,
            28, 29, 28, 29, 30, 31, 32, 1 };

    private static final int[][][] S_box = {
            //S1
            { { 14, 4, 13, 1, 2, 15, 11, 8, 3, 10, 6, 12, 5, 9, 0, 7 },
                    { 0, 15, 7, 4, 14, 2, 13, 1, 10, 6, 12, 11, 9, 5, 3, 8 },
                    { 4, 1, 14, 8, 13, 6, 2, 11, 15, 12, 9, 7, 3, 10, 5, 0 },
                    { 15, 12, 8, 2, 4, 9, 1, 7, 5, 11, 3, 14, 10, 0, 6, 13 } },
            //S2
            { { 15, 1, 8, 14, 6, 11, 3, 4, 9, 7, 2, 13, 12, 0, 5, 10 },
                    { 3, 13, 4, 7, 15, 2, 8, 14, 12, 0, 1, 10, 6, 9, 11, 5 },
                    { 0, 14, 7, 11, 10, 4, 13, 1, 5, 8, 12, 6, 9, 3, 2, 15 },
                    { 13, 8, 10, 1, 3, 15, 4, 2, 11, 6, 7, 12, 0, 5, 14, 9 } },
            //S3
            { { 10, 0, 9, 14, 6, 3, 15, 5, 1, 13, 12, 7, 11, 4, 2, 8 },
                    { 13, 7, 0, 9, 3, 4, 6, 10, 2, 8, 5, 14, 12, 11, 15, 1 },
                    { 13, 6, 4, 9, 8, 15, 3, 0, 11, 1, 2, 12, 5, 10, 14, 7 },
                    { 1, 10, 13, 0, 6, 9, 8, 7, 4, 15, 14, 3, 11, 5, 2, 12 } },
            //S4
            { { 7, 13, 14, 3, 0, 6, 9, 10, 1, 2, 8, 5, 11, 12, 4, 15 },
                    { 13, 8, 11, 5, 6, 15, 0, 3, 4, 7, 2, 12, 1, 10, 14, 9 },
                    { 10, 6, 9, 0, 12, 11, 7, 13, 15, 1, 3, 14, 5, 2, 8, 4 },
                    { 3, 15, 0, 6, 10, 1, 13, 8, 9, 4, 5, 11, 12, 7, 2, 14 } },
            //S5
            { { 2, 12, 4, 1, 7, 10, 11, 6, 8, 5, 3, 15, 13, 0, 14, 9 },
                    { 14, 11, 2, 12, 4, 7, 13, 1, 5, 0, 15, 10, 3, 9, 8, 6 },
                    { 4, 2, 1, 11, 10, 13, 7, 8, 15, 9, 12, 5, 6, 3, 0, 14 },
                    { 11, 8, 12, 7, 1, 14, 2, 13, 6, 15, 0, 9, 10, 4, 5, 3 } },
            //S6
            { { 12, 1, 10, 15, 9, 2, 6, 8, 0, 13, 3, 4, 14, 7, 5, 11 },
                    { 10, 15, 4, 2, 7, 12, 9, 5, 6, 1, 13, 14, 0, 11, 3, 8 },
                    { 9, 14, 15, 5, 2, 8, 12, 3, 7, 0, 4, 10, 1, 13, 11, 6 },
                    { 4, 3, 2, 12, 9, 5, 15, 10, 11, 14, 1, 7, 6, 0, 8, 13 } },
            //S7
            { { 4, 11, 2, 14, 15, 0, 8, 13, 3, 12, 9, 7, 5, 10, 6, 1 },
                    { 13, 0, 11, 7, 4, 9, 1, 10, 14, 3, 5, 12, 2, 15, 8, 6 },
                    { 1, 4, 11, 13, 12, 3, 7, 14, 10, 15, 6, 8, 0, 5, 9, 2 },
                    { 6, 11, 13, 8, 1, 4, 10, 7, 9, 5, 0, 15, 14, 2, 3, 12 } },
            //S8
            { { 13, 2, 8, 4, 6, 15, 11, 1, 10, 9, 3, 14, 5, 0, 12, 7 },
                    { 1, 15, 13, 8, 10, 3, 7, 4, 12, 5, 6, 11, 0, 14, 9, 2 },
                    { 7, 11, 4, 1, 9, 12, 14, 2, 0, 6, 10, 13, 15, 3, 5, 8 },
                    { 2, 1, 14, 7, 4, 10, 8, 13, 15, 12, 9, 0, 3, 5, 6, 11 } } };

    byte[][] K = new  byte[16][48];

    private static final int[] PP = {
            16, 7, 20, 21, 29, 12, 28, 17,
            1, 15, 23, 26, 5, 18, 31, 10,
            2, 8, 24, 14, 32, 27, 3, 9,
            19, 13, 30, 6, 22, 11, 4, 25 };


    private byte[] Initial_Permutation(byte[] pt)
    {
        byte[] tmp = new byte[64];

        for (int i = 0; i < 64; i++)
            tmp[i] = pt[IP[i] - 1];

        return tmp;
    }

    private byte[] Final_Permutation(byte[] pt)
    {
        byte[] tmp = new byte[64];

        for (int i = 0; i < 64; i++)
            tmp[i] = pt[FP[i] - 1];

        return tmp;
    }

    private byte[] P_Box_Permutation(byte[] x)
    {
        byte[] tmp = new byte[32];

        for (int i = 0; i < 32; i++)
            tmp[i] = x[PP[i] - 1];

        return tmp;
    }

    private void Generate_K(byte[] SK)
    {
        byte[] L = new byte[60];
        byte[] R = new byte[60];
        int i, j, st;

        for (i = 0; i < 28; i++){
            L[i + 28] = L[i] = SK[KP[i] - 1];
            R[i + 28] = R[i] = SK[KP[i + 28] - 1];
        }

        st = 0;

        for (i = 0; i < 16; i++) {
            st += KM[i];
            for (j = 0; j < 48; j++)
                K[i][j] = (CP[j] < 28) ? L[CP[j] + st - 1] : R[CP[j] - 29 + st];
        }

        return;
    }

    private void Work(byte[] L, byte[] R, byte[] KEY, int cnt)
    {
        byte[] X    = new byte[48];
        byte[] RES  = new byte[32];

        int i, j, a, b, st, st2, tmp;

        for (i = 0; i < 48; i++)
            X[i] = (byte) (R[EP[i] - 1] ^ KEY[i]);

        for (i = 0; i < 8; i++) {
            st = i * 6;
            a = (X[st] << 1) + X[st + 5];
            b = (X[st + 1] << 3) + (X[st + 2] << 2) + (X[st + 3] << 1) + X[st + 4];
            tmp = S_box[i][a][b];
            st2 = i * 4;
            for (j = 0; j < 4; j++)
                RES[st2 + (3 - j)] |= (tmp >> j) & 1;
        }

        RES = P_Box_Permutation(RES);

        for (i = 0; i < 32; i++)
            L[i] ^= RES[i];

        if (cnt == 15) return;

        byte[] temp = new byte[32];

        System.arraycopy(L,0,temp,0,32);
        System.arraycopy(R,0,L,0,32);
        System.arraycopy(temp,0,R,0,32);

        return;
    }

    public void setKey(byte[] key){
        byte[] sk = new byte[64];

        for (int i = 0; i < 8; i++)
            for (int j = 7; j >= 0; j--)
                sk[i * 8 + (7 - j)] = (byte) ((key[i] >> j) & 1);

        Generate_K(sk);
    }

    public void encrypt(byte[] orgData, byte[] encryptData)
    {
        int i, j;

        byte[] L  = new byte[32];
        byte[] R  = new byte[32];
        byte[] pt = new byte[64];

        for (i = 0; i < 8; i++)
            for (j = 7; j >= 0; j--)
                pt[i * 8 + (7 - j)] = (byte) ((orgData[i] >> j) & 1);

        pt = Initial_Permutation(pt);

        for (i = 0; i < 32; i++){
            L[i] = pt[i];
            R[i] = pt[i + 32];
        }


        for (i = 0; i < 16; i++){
            byte[] temp = new byte[48];
            System.arraycopy(K[i],0,temp,0,48);
            Work(L, R, temp, i);
        }


        for (i = 0; i < 32; i++){
            pt[i]       = L[i];
            pt[i + 32]  = R[i];
        }

        pt = Final_Permutation(pt);

        for(i = 0;i < 8;i++){
            encryptData[i] = 0;
        }

        for (i = 0; i < 8; i++)
            for (j = 7; j >= 0; j--)
                encryptData[i] |= pt[i * 8 + (7 - j)] << j;

        return;
    }

    public void decrypt(byte[] encryptData, byte[] decryData){
        int n = encryptData.length / 8;
        byte[] tempIn   = new byte[8];
        byte[] tempOut  = new byte[8];

        for (int i = 0;i < n;i++){
            if((i + 1) * 8 > decryData.length){
                return;
            }

            System.arraycopy(encryptData,i * 8,tempIn,0,8);
            decrypt8(tempIn,tempOut);
            System.arraycopy(tempOut,0,decryData,i * 8,8);
        }
    }

    public void decrypt8(byte[] encryptData, byte[] decryptData)
    {
        int i, j;
        byte[] L  = new byte[32];
        byte[] R  = new byte[32];
        byte[] pt = new byte[64];

        for (i = 0; i < 8; i++)
            for (j = 7; j >= 0; j--)
                pt[i * 8 + (7 - j)] = (byte) ((encryptData[i] >> j) & 1);

        pt = Initial_Permutation(pt);

        for (i = 0; i < 32; i++){
            L[i] = pt[i];
            R[i] = pt[i + 32];
        }

        for (i = 0; i < 16; i++){
            byte[] temp = new byte[48];
            System.arraycopy(K[15 - i],0,temp,0,48);
            Work(L, R, temp, i);
        }


        for (i = 0; i < 32; i++){
            pt[i] = L[i];
            pt[i + 32] = R[i];
        }


        pt = Final_Permutation(pt);

        for(i = 0;i < 8;i++){
            decryptData[i] = 0;
        }

        for (i = 0; i < 8; i++)
            for (j = 7; j >= 0; j--)
                decryptData[i] |= pt[i * 8 + (7 - j)] << j;

        return;
    }
}
