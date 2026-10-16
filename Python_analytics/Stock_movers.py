import os

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import pandas as pd
from dotenv import load_dotenv
from sqlalchemy import create_engine


load_dotenv()

DB_USERNAME = os.getenv('DB_USERNAME')
DB_PASSWORD = os.getenv('DB_PASSWORD')
engine = create_engine(
    f'postgresql+psycopg2://{DB_USERNAME}:{DB_PASSWORD}@localhost:25432/ribbit-dw'
)


sql_query = """
WITH latest_instrument_activity AS (
    SELECT
        ticker,
        price,
        average_price,
        ROW_NUMBER() OVER (
            PARTITION BY instrument_id
            ORDER BY quote_time DESC, order_created_at DESC, activity_id DESC
        ) AS quote_rank
    FROM dw.ribbit_trade_activity
    WHERE price > 0
      AND average_price > 0
),
market_movers AS (
    SELECT
        ticker,
        price AS current_price,
        average_price AS reference_price,
        price - average_price AS price_change,
        100.0 * (price - average_price)
            / NULLIF(average_price, 0) AS percent_change
    FROM latest_instrument_activity
    WHERE quote_rank = 1
)
SELECT *
FROM market_movers
WHERE percent_change > 0
ORDER BY percent_change DESC
LIMIT 5;
"""

try:
    movers = pd.read_sql(sql_query, engine)

    if movers.empty:
        raise RuntimeError('No gainers were found in the warehouse.')

    print('Top 5 Gainers for the Day:')
    print('-' * 60)
    print(
        movers[
            ['ticker', 'current_price', 'reference_price', 'percent_change']
        ].to_string(index=False)
    )
    print('-' * 60)

    movers = movers.sort_values('percent_change', ascending=True)
    figure, axis = plt.subplots(figsize=(11, 7), facecolor='#111827')
    axis.set_facecolor('#111827')
    bars = axis.bar(
        movers['ticker'],
        movers['percent_change'],
        color='#b7f000',
        edgecolor='#d9ff66',
        linewidth=1.2,
        width=0.62,
    )

    for bar, change in zip(bars, movers['percent_change']):
        axis.text(
            bar.get_x() + bar.get_width() / 2,
            change + 2,
            f'+{change:.2f}%',
            ha='center',
            va='bottom',
            color='#f3f4f6',
            fontweight='bold',
        )

    axis.set_title(
        'Top 5 Gainers for the Day',
        fontsize=18,
        fontweight='bold',
        color='#f9fafb',
        pad=20,
    )
    axis.set_xlabel('Stock', color='#d1d5db', labelpad=12)
    axis.set_ylabel('Gain from Average Price (%)', color='#d1d5db', labelpad=12)
    axis.tick_params(colors='#d1d5db')
    axis.grid(axis='y', color='#374151', alpha=0.8, linestyle='--')
    axis.set_axisbelow(True)
    axis.spines[['top', 'right', 'left']].set_visible(False)
    axis.spines['bottom'].set_color('#4b5563')
    axis.set_ylim(0, movers['percent_change'].max() * 1.18)
    figure.tight_layout()

    script_dir = os.path.dirname(os.path.abspath(__file__))
    output_path = os.path.join(script_dir, 'top_5_market_movers.png')
    plt.savefig(output_path, dpi=300, bbox_inches='tight')
    plt.close()
    print(f'Chart saved to {output_path}')

except Exception as error:
    print(f'Error: {error}')
    raise
