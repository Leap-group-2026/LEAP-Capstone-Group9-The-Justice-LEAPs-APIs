import os

import matplotlib
matplotlib.use('Agg')
import pandas as pd
import matplotlib.pyplot as plt
from sqlalchemy import create_engine
from dotenv import load_dotenv



# Load environment variables from .env file
load_dotenv()

# Get database credentials from environment variables
DB_USERNAME = os.getenv('DB_USERNAME')
DB_PASSWORD = os.getenv('DB_PASSWORD')

# Database connection setup
engine = create_engine(f'postgresql+psycopg2://{DB_USERNAME}:{DB_PASSWORD}@localhost:25432/ribbit-dw')

try:
    # Count completed trades from the warehouse and rank stocks by quantity.
    sql_query = """
    SELECT
        ticker,
        SUM(quantity) AS total_volume
    FROM dw.ribbit_trade_activity
    WHERE status = 'FILLED'
            AND side = 'BUY'
    GROUP BY ticker
    ORDER BY total_volume DESC
    LIMIT 20;
    """

    # Fetch the data from database
    df = pd.read_sql(sql_query, engine)

    # Print the data to console
    print("Top 20 Most Traded Stocks:")
    print("-" * 60)
    print(df.to_string(index=False))
    print("-" * 60)

    # Create a readable dark-mode chart for twenty tickers.
    df = df.sort_values('total_volume')
    figure, axis = plt.subplots(figsize=(12, 10), facecolor='#111827')
    axis.set_facecolor('#111827')
    bars = axis.barh(
        df['ticker'],
        df['total_volume'],
        color='#b7f000',
        edgecolor='#d9ff66',
        linewidth=0.8,
        height=0.72,
    )

    # Add volume numbers on top of each bar
    for bar in bars:
        width = bar.get_width()
        axis.text(
            width,
            bar.get_y() + bar.get_height() / 2,
            f'  {int(width):,}',
            ha='left',
            va='center',
            color='#f9fafb',
            fontsize=9,
            fontweight='bold',
        )

    axis.set_title('Top 20 Most Traded Stocks', fontsize=18, fontweight='bold', color='#f9fafb', pad=20)
    axis.set_xlabel('Completed Shares Traded', fontsize=12, color='#d1d5db', labelpad=12)
    axis.set_ylabel('Stock Ticker', fontsize=12, color='#d1d5db', labelpad=12)
    axis.tick_params(colors='#d1d5db', labelsize=10)
    axis.grid(axis='x', color='#374151', alpha=0.8, linestyle='--')
    axis.set_axisbelow(True)
    axis.spines[['top', 'right', 'left']].set_visible(False)
    axis.spines['bottom'].set_color('#4b5563')
    axis.set_xlim(0, df['total_volume'].max() * 1.12)
    figure.tight_layout()

    # Save and display the chart
    script_dir = os.path.dirname(os.path.abspath(__file__))
    output_path = os.path.join(script_dir, 'top_20_traded_stocks.png')
    figure.savefig(output_path, dpi=300, bbox_inches='tight', facecolor=figure.get_facecolor())
    print(f"\nChart saved to {output_path}")
    plt.close(figure)

except Exception as e:
    print(f"Error: {e}")

print("\nDone!")