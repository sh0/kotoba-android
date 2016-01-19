/*
 * Kotoba-chan
 *
 * Copyright (C) 2013 Siim Meerits <sh0@yutani.ee>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

// Package
package ee.yutani.kotoba;

// Constraint optimizer class
public class QuadraticOptimizer
{
    // Constants
    static final float m_wolfe_gamma = 0.1f;
    static final float m_sigma_mul = 10.0f;
    static final int m_penalty_runs = 5;
    static final int m_newton_runs = 20;
    static final int m_gs_runs = 20;

    // Variables
    float[][] m_a;
    float[] m_b;

    // Constructor
    public QuadraticOptimizer(float[][] a, float[] b)
    {
        // Variables
        m_a = a;
        m_b = b;

        // Check
        assert(m_b.length == m_a.length);
    }

    // Calculate
    public void calculate(float[] x)
    {
        // Check if calculation needed
        if (phi(1.0f, x) == 0.0f)
            return;

        // Debug
        //Log.w("calculate", "<===================================================>");
        //Log.w("calculate", "string = [ " + str + "]");

        // Calculate
        float sigma = 1.0f;
        for (int k = 0; k < m_penalty_runs; k++) {
            newton_solve(x, sigma);
            sigma *= m_sigma_mul;
        }
    }

    private void newton_solve(float[] x, float sigma)
    {
        for (int i = 0; i < m_newton_runs; i++)
            newton_iteration(x, sigma);
    }

    private void newton_iteration(float[] x, float sigma)
    {
        // Calculate gradient
        float[] d = new float[x.length];
        for (int i = 0; i < d.length; i++)
            d[i] = phi_d1(i, sigma, x);

        // Calculate Hessian matrix (symmetric)
        float[][] h = new float[x.length][x.length];
        for (int i = 0; i < h.length; i++)
            for (int j = i; j < h[0].length; j++)
                h[i][j] = phi_d2(i, j, sigma, x);
        for (int i = 0; i < h.length; i++)
            for (int j = 0; j < i; j++)
                h[i][j] = h[j][i];


        /*
        // Debug
        //Log.w("newton_solve", "<========================================>");
        Log.w("newton_solve", String.format("phi = %f", phi(sigma, x)));

        // Debug
        String str = "";
        for (int i = 0; i < d.length; i++)
            str += String.format("%.3f ", d[i]);
        Log.w("newton_solve", "d = [ " + str + "]");

        // Debug
        for (int i = 0; i < h.length; i++) {
            str = "";
            for (int j = 0; j < h[0].length; j++)
                str += String.format("%.3f ", h[i][j]);
            Log.w("newton_solve", String.format("h[%02d] = [ %s]", i, str));
        }
        */

        // Linear system solver
        float p[] = gs_solver(h, d);

        // Iteration
        for (int i = 0; i < x.length; i++)
            x[i] = x[i] - (m_wolfe_gamma * p[i]);

        /*
        // Diagonal matrix inverse
        for (int i = 0; i < h.length; i++)
            h[i][i] = 1.0f / h[i][i];

        // Iteration
        for (int i = 0; i < x.length; i++)
            x[i] = x[i] - (m_wolfe_gamma * dot(h[i], d));
        */

        /*
        // Debug
        str = "";
        for (int i = 0; i < x.length; i++)
            str += String.format("%.3f ", x[i]);
        Log.w("newton_solve", "x = [ " + str + "]");
        */
    }

    // Gauss-Seidel solver
    private float[] gs_solver(float[][] a, float[] b)
    {
        // Initial guess
        float p[] = new float[b.length];
        for (int i = 0; i < p.length; i++)
            p[i] = 1.0f;

        for (int z = 0; z < m_gs_runs; z++) {
            for (int i = 0; i < p.length; i++) {
                float s = 0.0f;
                for (int j = 0; j < p.length; j++) {
                    if (i != j)
                        s += a[i][j] * p[j];
                }
                p[i] = (b[i] - s) / a[i][i];
            }
        }

        // Result
        return p;
    }

    // Math
    private float dot(float[] a, float[] b)
    {
        assert(a.length == b.length);
        float r = 0.0f;
        for (int i = 0; i < a.length; i++)
            r += a[i] * b[i];
        return r;
    }

    // Cost function f(x)
    private float f(float[] x)
    {
        return dot(x, x);
    }

    // Cost function phi(x)
    private float phi(float sigma, float[] x)
    {
        float r = 0.0f;
        for (int i = 0; i < x.length; i++)
            r += Math.pow(Math.min(0, dot(m_a[i], x) - m_b[i]), 2.0f);
        return f(x) + (sigma * r);
    }

    private float phi_d1(int n, float sigma, float[] x)
    {
        float r = 0.0f;
        for (int i = 0; i < m_a.length; i++) {
            float c = dot(m_a[i], x) - m_b[i];
            if (c < 0)
                r += 2.0f * m_a[i][n] * c;
        }
        return (2.0f * x[n]) + (sigma * r);
    }

    private float phi_d2(int n, int m, float sigma, float[] x)
    {
        float r = 0.0f;
        for (int i = 0; i < m_a.length; i++) {
            float c = dot(m_a[i], x) - m_b[i];
            if (c < 0)
                r += 2.0f * m_a[i][n] * m_a[i][m];
        }
        return ((n == m) ? 2.0f : 0.0f) + (sigma * r);
    }
}
